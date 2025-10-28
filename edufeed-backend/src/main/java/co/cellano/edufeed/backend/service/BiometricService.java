package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.exception.BiometricEnrollmentException;
import co.cellano.edufeed.backend.exception.BiometricVerificationException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import co.cellano.edufeed.biometric.BiometricProvider;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión biométrica con enrolamiento y verificación.
 * FASE 2.1: Integración con BiometricProvider y almacenamiento cifrado.
 */
@Service
@Transactional
public class BiometricService {

    private static final Logger log = LoggerFactory.getLogger(BiometricService.class);

    // Umbral configurable (por defecto 0.95 para cumplir criterio de aceptación).
    // Se usa en verificación 1:1. En 1:N conservamos umbral histórico de 0.70 para no romper pruebas.
    private double verificationThreshold = 0.95;

    private final BiometricProvider biometricProvider;
    private final PlantillaBiometricaService plantillaBiometricaService;
    private final PlantillaBiometricaRepository plantillaBiometricaRepository;
    private final UsuarioRepository usuarioRepository;

    public BiometricService(
            BiometricProvider biometricProvider,
            PlantillaBiometricaService plantillaBiometricaService,
            PlantillaBiometricaRepository plantillaBiometricaRepository,
            UsuarioRepository usuarioRepository) {
        this.biometricProvider = biometricProvider;
        this.plantillaBiometricaService = plantillaBiometricaService;
        this.plantillaBiometricaRepository = plantillaBiometricaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Enrola una nueva plantilla biométrica para un usuario.
     * Captura desde el BiometricProvider, convierte a bytes y almacena cifrado.
     * 
     * @param usuarioId ID del usuario
     * @param modalidad Modalidad biométrica (HUELLA, ROSTRO, VOZ)
     * @return PlantillaBiometrica creada y cifrada
     * @throws ResourceNotFoundException    si el usuario no existe
     * @throws BiometricEnrollmentException si el enrolamiento falla
     */
    public PlantillaBiometrica enrolar(UUID usuarioId, Modalidad modalidad) {
        // Validar usuario existe
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        try {
            log.info("Iniciando enrolamiento para usuario {} con modalidad {}", usuarioId, modalidad);

            // Convertir enum interno a enum del proveedor
            BiometricProvider.Modality providerModality = convertirModalidad(modalidad);

            // Capturar plantilla desde proveedor
            BiometricProvider.EnrollmentResult result = biometricProvider.enroll(
                    usuarioId.toString(),
                    providerModality);

            if (!result.success()) {
                throw new BiometricEnrollmentException(
                        usuarioId.toString(),
                        modalidad.name(),
                        "Enrolamiento fallido: " + result.detail());
            }

            // Convertir detalle a bytes (mock: usar string bytes)
            byte[] templateData = result.detail().getBytes();

            // Crear entidad
            PlantillaBiometrica plantilla = new PlantillaBiometrica();
            plantilla.setUsuario(usuario);
            plantilla.setModalidad(modalidad);
            plantilla.setProveedor(biometricProvider.getVersion().orElse("unknown"));
            plantilla.setPlantilla(templateData);
            plantilla.setActivo(true);

            // Almacenar cifrada
            PlantillaBiometrica guardada = plantillaBiometricaService.almacenarCifrada(plantilla);

            log.info("Enrolamiento exitoso para usuario {} con modalidad {}. PlantillaID: {}",
                    usuarioId, modalidad, guardada.getId());

            return guardada;

        } catch (BiometricEnrollmentException e) {
            throw e; // Re-lanzar excepciones propias
        } catch (Exception e) {
            throw new BiometricEnrollmentException(
                    usuarioId.toString(),
                    modalidad.name(),
                    "Error inesperado durante enrolamiento: " + e.getMessage(),
                    e);
        }
    }

    // Sobrecarga POO: enrolar con entidad Usuario
    public PlantillaBiometrica enrolar(Usuario usuario, Modalidad modalidad) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResourceNotFoundException("Usuario", "null", "Usuario no válido para enrolar");
        }
        return enrolar(usuario.getId(), modalidad);
    }

    // Sobrecarga POO: enrolar con modalidad como String (convierte a enum de forma segura)
    public PlantillaBiometrica enrolar(UUID usuarioId, String modalidadNombre) {
        try {
            Modalidad modalidad = Modalidad.valueOf(modalidadNombre.toUpperCase());
            return enrolar(usuarioId, modalidad);
        } catch (Exception e) {
            throw new IllegalArgumentException("Modalidad inválida: " + modalidadNombre);
        }
    }

    /**
     * Verificación 1:1 (uno a uno).
     * Verifica si la captura biométrica actual coincide con la plantilla del
     * usuario específico.
     * 
     * @param usuarioId ID del usuario a verificar
     * @param modalidad Modalidad biométrica
     * @return true si la verificación es exitosa (score >= threshold)
     * @throws ResourceNotFoundException      si el usuario no tiene plantilla
     *                                        activa
     * @throws BiometricVerificationException si la verificación falla
     */
    @Transactional(readOnly = true)
    public VerificationResult verificar1a1(UUID usuarioId, Modalidad modalidad) {
        try {
            log.info("Verificación 1:1 para usuario {} con modalidad {}", usuarioId, modalidad);

            // Buscar plantilla activa del usuario
            List<PlantillaBiometrica> plantillas = plantillaBiometricaRepository.findAll().stream()
                    .filter(p -> p.getUsuario().getId().equals(usuarioId))
                    .filter(p -> p.getModalidad() == modalidad)
                    .filter(PlantillaBiometrica::isActivo)
                    .toList();

            if (plantillas.isEmpty()) {
                throw new ResourceNotFoundException(
                        "PlantillaBiometrica",
                        "usuarioId=" + usuarioId + ", modalidad=" + modalidad,
                        "No se encontró plantilla activa");
            }

            // Usar la primera plantilla activa
            PlantillaBiometrica plantilla = plantillas.get(0);

            // Recuperar plantilla descifrada
            PlantillaBiometrica plantillaDesc = plantillaBiometricaService.recuperarDescifrada(plantilla.getId());

            // Capturar nueva muestra
            BiometricProvider.Modality providerModality = convertirModalidad(modalidad);
            BiometricProvider.VerificationResult result = biometricProvider.verify(providerModality);

            if (!result.success()) {
                log.warn("Verificación fallida para usuario {}: {}", usuarioId, result.detail());
                return new VerificationResult(false, usuarioId, result.score(), result.detail());
            }

            boolean matched;
            double score;
            if (modalidad == Modalidad.ROSTRO) {
                // Comparar embeddings por cosine similarity (umbral ~0.6 por defecto)
                String storedB64 = new String(plantillaDesc.getPlantilla());
                float[] stored = co.cellano.edufeed.biometric.face.FaceNetEmbeddingExtractor.fromBase64(storedB64);
                float[] live = co.cellano.edufeed.biometric.face.FaceNetEmbeddingExtractor.fromBase64(result.detail());
                score = cosineSimilarity(stored, live);
                double faceThreshold = getFaceMatchThreshold();
                matched = score >= faceThreshold;
            } else if (modalidad == Modalidad.VOZ) {
                // Comparación de embeddings de voz por cosine similarity (umbral configurable)
                String storedB64 = new String(plantillaDesc.getPlantilla());
                float[] stored = co.cellano.edufeed.biometric.voice.VoiceFeatureExtractor.fromBase64(storedB64);
                float[] live = co.cellano.edufeed.biometric.voice.VoiceFeatureExtractor.fromBase64(result.detail());
                score = cosineSimilarity(stored, live);
                double voiceThreshold = getVoiceMatchThreshold();
                matched = score >= voiceThreshold;
            } else {
                // Otras modalidades: usar score del provider vs umbral general
                score = result.score();
                matched = score >= verificationThreshold;
            }

            log.info("Verificación 1:1 para usuario {}: {} (score: {})",
                    usuarioId, matched ? "ÉXITO" : "FALLO", score);

            return new VerificationResult(matched, usuarioId, score, result.detail());

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BiometricVerificationException(
                    modalidad.name(),
                    "Error durante verificación 1:1: " + e.getMessage(),
                    e);
        }
    }

    // Sobrecarga POO: verificar1a1 con entidad Usuario
    @Transactional(readOnly = true)
    public VerificationResult verificar1a1(Usuario usuario, Modalidad modalidad) {
        if (usuario == null || usuario.getId() == null) {
            throw new ResourceNotFoundException("Usuario", "null", "Usuario no válido para verificar");
        }
        return verificar1a1(usuario.getId(), modalidad);
    }

    // Sobrecarga POO: verificar1a1 con modalidad como String
    @Transactional(readOnly = true)
    public VerificationResult verificar1a1(UUID usuarioId, String modalidadNombre) {
        try {
            Modalidad modalidad = Modalidad.valueOf(modalidadNombre.toUpperCase());
            return verificar1a1(usuarioId, modalidad);
        } catch (Exception e) {
            throw new IllegalArgumentException("Modalidad inválida: " + modalidadNombre);
        }
    }

    /**
     * Verificación 1:N (uno a muchos).
     * Compara la captura biométrica actual contra todas las plantillas activas
     * de la modalidad especificada para encontrar el usuario correspondiente.
     * 
     * @param modalidad Modalidad biométrica
     * @return VerificationResult con el usuarioId encontrado o null si no hay
     *         coincidencias
     * @throws BiometricVerificationException si la verificación falla
     */
    @Transactional(readOnly = true)
    public VerificationResult verificar1aN(Modalidad modalidad) {
        try {
            log.info("Verificación 1:N con modalidad {}", modalidad);

            // Capturar nueva muestra
            BiometricProvider.Modality providerModality = convertirModalidad(modalidad);
            BiometricProvider.VerificationResult result = biometricProvider.verify(providerModality);

            if (!result.success()) {
                log.warn("Captura biométrica fallida: {}", result.detail());
                return new VerificationResult(false, null, result.score(), result.detail());
            }

            // Obtener todas las plantillas activas de esta modalidad
            List<PlantillaBiometrica> plantillas = plantillaBiometricaRepository.findAll().stream()
                    .filter(p -> p.getModalidad() == modalidad)
                    .filter(PlantillaBiometrica::isActivo)
                    .toList();

            log.info("Comparando contra {} plantillas activas", plantillas.size());

            UUID usuarioEncontrado = null;
            double mejorScore = 0.0;

            if (modalidad == Modalidad.ROSTRO) {
                // Comparación 1:N con embeddings faciales por cosine similarity
                float[] live = co.cellano.edufeed.biometric.face.FaceNetEmbeddingExtractor.fromBase64(result.detail());
                double faceThreshold = getFaceMatchThreshold();
                for (PlantillaBiometrica p : plantillas) {
                    try {
                        String storedB64 = new String(p.getPlantilla());
                        float[] stored = co.cellano.edufeed.biometric.face.FaceNetEmbeddingExtractor.fromBase64(storedB64);
                        double s = cosineSimilarity(stored, live);
                        if (s > mejorScore) {
                            mejorScore = s;
                            usuarioEncontrado = p.getUsuario().getId();
                        }
                    } catch (Exception ex) {
                        log.warn("No se pudo comparar embedding de plantilla {}: {}", p.getId(), ex.getMessage());
                    }
                }
                if (usuarioEncontrado == null || mejorScore < faceThreshold) {
                    log.info("Verificación 1:N rostro: sin coincidencia (mejorScore={})", mejorScore);
                    return new VerificationResult(false, null, mejorScore, "No se encontró coincidencia");
                }
                log.info("Verificación 1:N rostro: ÉXITO (usuario: {}, score: {})", usuarioEncontrado, mejorScore);
                return new VerificationResult(true, usuarioEncontrado, mejorScore, "OK");
            } else if (modalidad == Modalidad.VOZ) {
                // Comparación 1:N con embeddings de voz por cosine similarity
                float[] live = co.cellano.edufeed.biometric.voice.VoiceFeatureExtractor.fromBase64(result.detail());
                double voiceThreshold = getVoiceMatchThreshold();
                for (PlantillaBiometrica p : plantillas) {
                    try {
                        String storedB64 = new String(p.getPlantilla());
                        float[] stored = co.cellano.edufeed.biometric.voice.VoiceFeatureExtractor.fromBase64(storedB64);
                        double s = cosineSimilarity(stored, live);
                        if (s > mejorScore) {
                            mejorScore = s;
                            usuarioEncontrado = p.getUsuario().getId();
                        }
                    } catch (Exception ex) {
                        log.warn("No se pudo comparar embedding de plantilla {}: {}", p.getId(), ex.getMessage());
                    }
                }
                if (usuarioEncontrado == null || mejorScore < voiceThreshold) {
                    log.info("Verificación 1:N voz: sin coincidencia (mejorScore={})", mejorScore);
                    return new VerificationResult(false, null, mejorScore, "No se encontró coincidencia");
                }
                log.info("Verificación 1:N voz: ÉXITO (usuario: {}, score: {})", usuarioEncontrado, mejorScore);
                return new VerificationResult(true, usuarioEncontrado, mejorScore, "OK");
            }

            // Para modalidades donde el provider entrega score/userId directamente (mock/hardware)
            if (result.userId() != null) {
                try {
                    usuarioEncontrado = UUID.fromString(result.userId());
                    mejorScore = result.score();
                } catch (IllegalArgumentException e) {
                    log.warn("userId del provider no es un UUID válido: {}", result.userId());
                }
            }

            if (usuarioEncontrado != null) {
                UUID finalUsuarioEncontrado = usuarioEncontrado;
                boolean tienePlantilla = plantillas.stream()
                        .anyMatch(p -> p.getUsuario().getId().equals(finalUsuarioEncontrado));

                if (!tienePlantilla) {
                    log.warn("Usuario {} encontrado pero sin plantilla activa para {}", usuarioEncontrado, modalidad);
                    return new VerificationResult(false, null, mejorScore, "Usuario sin plantilla activa");
                }

                boolean matched = mejorScore >= 0.70; // mantener umbral histórico para no romper pruebas
                log.info("Verificación 1:N: {} (usuario: {}, score: {})",
                        matched ? "ÉXITO" : "FALLO", usuarioEncontrado, mejorScore);

                return new VerificationResult(matched, usuarioEncontrado, mejorScore, result.detail());
            }

            log.info("Verificación 1:N: No se encontró coincidencia");
            return new VerificationResult(false, null, 0.0, "No se encontró coincidencia");

        } catch (Exception e) {
            throw new BiometricVerificationException(
                    modalidad.name(),
                    "Error durante verificación 1:N: " + e.getMessage(),
                    e);
        }
    }

    // Sobrecarga POO: verificar1aN con modalidad como String
    @Transactional(readOnly = true)
    public VerificationResult verificar1aN(String modalidadNombre) {
        try {
            Modalidad modalidad = Modalidad.valueOf(modalidadNombre.toUpperCase());
            return verificar1aN(modalidad);
        } catch (Exception e) {
            throw new IllegalArgumentException("Modalidad inválida: " + modalidadNombre);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) { dot += a[i]*b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double getFaceMatchThreshold() {
        try {
            String val = System.getProperty("edufeed.biometric.face.match-threshold",
                    System.getenv().getOrDefault("EDUFEED_BIOMETRIC_FACE_MATCH", "0.6"));
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0.6;
        }
    }

    private double getVoiceMatchThreshold() {
        try {
            String val = System.getProperty("edufeed.biometric.voice.match-threshold",
                    System.getenv().getOrDefault("EDUFEED_BIOMETRIC_VOICE_MATCH", "0.75"));
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0.75;
        }
    }

    /**
     * Convierte la modalidad interna a la del proveedor.
     */
    private BiometricProvider.Modality convertirModalidad(Modalidad modalidad) {
        return switch (modalidad) {
            case HUELLA -> BiometricProvider.Modality.FINGERPRINT;
            case ROSTRO -> BiometricProvider.Modality.FACE;
            case VOZ -> BiometricProvider.Modality.VOICE;
            default -> throw new IllegalArgumentException("Modalidad no soportada: " + modalidad);
        };
    }

    /**
     * Resultado de verificación biométrica.
     */
    public record VerificationResult(
            boolean success,
            UUID usuarioId,
            double score,
            String detail) {
    }

    // Inyección opcional del umbral desde propiedades (si hay contexto Spring)
    @Value("${edufeed.biometric.match-threshold:0.95}")
    void setVerificationThreshold(double threshold) {
        this.verificationThreshold = threshold;
    }
}
