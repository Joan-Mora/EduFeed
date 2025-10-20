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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de gestión biométrica con enrolamiento y verificación.
 * FASE 2.1: Integración con BiometricProvider y almacenamiento cifrado.
 */
@Service
@Transactional
public class BiometricService {

    private static final Logger log = LoggerFactory.getLogger(BiometricService.class);

    private static final double VERIFICATION_THRESHOLD = 0.70; // Umbral de similitud (70%)

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

            // Recuperar plantilla descifrada (en implementación real se usaría para
            // comparación)
            // Por ahora solo validamos que existe y es descifrable
            plantillaBiometricaService.recuperarDescifrada(plantilla.getId());

            // Capturar nueva muestra
            BiometricProvider.Modality providerModality = convertirModalidad(modalidad);
            BiometricProvider.VerificationResult result = biometricProvider.verify(providerModality);

            if (!result.success()) {
                log.warn("Verificación fallida para usuario {}: {}", usuarioId, result.detail());
                return new VerificationResult(false, usuarioId, result.score(), result.detail());
            }

            // Comparar (en mock, siempre retorna score alto; en real, comparar plantillas)
            boolean matched = result.score() >= VERIFICATION_THRESHOLD;

            log.info("Verificación 1:1 para usuario {}: {} (score: {})",
                    usuarioId, matched ? "ÉXITO" : "FALLO", result.score());

            return new VerificationResult(matched, usuarioId, result.score(), result.detail());

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BiometricVerificationException(
                    modalidad.name(),
                    "Error durante verificación 1:1: " + e.getMessage(),
                    e);
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

            // En modo MOCK, el provider ya retorna un userId
            // En modo REAL, comparar contra todas las plantillas
            UUID usuarioEncontrado = null;
            double mejorScore = 0.0;

            // Para mock: extraer userId del resultado
            if (result.userId() != null) {
                try {
                    usuarioEncontrado = UUID.fromString(result.userId());
                    mejorScore = result.score();
                } catch (IllegalArgumentException e) {
                    log.warn("userId del provider no es un UUID válido: {}", result.userId());
                }
            }

            // Validar que el usuario encontrado tenga plantilla activa
            if (usuarioEncontrado != null) {
                UUID finalUsuarioEncontrado = usuarioEncontrado;
                boolean tienePlantilla = plantillas.stream()
                        .anyMatch(p -> p.getUsuario().getId().equals(finalUsuarioEncontrado));

                if (!tienePlantilla) {
                    log.warn("Usuario {} encontrado pero sin plantilla activa para {}", usuarioEncontrado, modalidad);
                    return new VerificationResult(false, null, mejorScore, "Usuario sin plantilla activa");
                }

                boolean matched = mejorScore >= VERIFICATION_THRESHOLD;
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
}
