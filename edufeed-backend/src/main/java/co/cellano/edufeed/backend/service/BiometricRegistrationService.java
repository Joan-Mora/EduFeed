package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el registro de datos biométricos.
 * Maneja sesiones de registro y almacenamiento de plantillas.
 */
@Service
public class BiometricRegistrationService {

    @Autowired
    private PlantillaBiometricaRepository plantillaRepository;

    @Autowired
    private co.cellano.edufeed.backend.repository.UsuarioRepository usuarioRepository;

    // Mapa para trackear sesiones de registro en memoria
    // En producción, usar Redis o base de datos
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();

    /**
     * Inicia una nueva sesión de registro biométrico
     */
    public String startSession(Usuario usuario) {
        String sessionId = UUID.randomUUID().toString();
        SessionData session = new SessionData();
        session.userId = usuario.getId().toString();
        session.startTime = LocalDateTime.now();
        session.huellaCompletada = false;
        session.rostroCompletado = false;
        session.vozCompletada = false;

        activeSessions.put(sessionId, session);

        System.out.println("[BiometricRegistration] Nueva sesión iniciada: " + sessionId + " para usuario: "
                + usuario.getNombreCompleto());

        return sessionId;
    }

    /**
     * Guarda datos de huella digital
     */
    public boolean saveFingerprint(Usuario usuario, String fingerprintData, String sessionId) {
        try {
            // WebAuthn envía un JSON con campos base64 (attestationObject, clientDataJSON).
            // En esta primera versión almacenamos el JSON tal cual en la columna bytea.
            // Si en el futuro se requiere verificación/parseo, se podrá descomponer.
            byte[] plantillaBytes;
            if (fingerprintData != null && fingerprintData.trim().startsWith("{")) {
                // JSON serializado (UTF-8)
                plantillaBytes = fingerprintData.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            } else {
                // Compatibilidad: si viene una cadena base64 “plana”, decodificarla
                plantillaBytes = Base64.getDecoder().decode(fingerprintData);
            }

            // Buscar si ya existe una plantilla de huella activa
            List<PlantillaBiometrica> existentes = plantillaRepository
                    .findByUsuarioAndModalidad(usuario, Modalidad.HUELLA);

            // Desactivar plantillas anteriores
            for (PlantillaBiometrica old : existentes) {
                if (old.isActivo()) {
                    old.setActivo(false);
                    plantillaRepository.save(old);
                }
            }

            // Crear nueva plantilla
            PlantillaBiometrica plantilla = new PlantillaBiometrica();
            plantilla.setUsuario(usuario);
            plantilla.setModalidad(Modalidad.HUELLA);
            plantilla.setPlantilla(plantillaBytes);
            plantilla.setProveedor("WebAuthn");
            plantilla.setActivo(true);
            plantilla.setCreadoEn(java.time.OffsetDateTime.now());

            plantillaRepository.save(plantilla);
            System.out.println("[BiometricRegistration] Huella registrada para: " + usuario.getNombreCompleto());

            // Actualizar sesión si existe
            if (sessionId != null && activeSessions.containsKey(sessionId)) {
                activeSessions.get(sessionId).huellaCompletada = true;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[BiometricRegistration] Error guardando huella: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Guarda datos de reconocimiento facial
     */
    public boolean saveFaceData(Usuario usuario, String faceData, String sessionId) {
        try {
            // Permitir JSON (descriptor) o base64 de imagen
            byte[] plantillaBytes;
            if (faceData != null && faceData.trim().startsWith("{")) {
                plantillaBytes = faceData.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            } else {
                String payload = faceData;
                if (payload != null && payload.startsWith("data:")) {
                    int comma = payload.indexOf(',');
                    payload = comma > 0 ? payload.substring(comma + 1) : payload;
                }
                plantillaBytes = Base64.getDecoder().decode(payload);
            }

            // Buscar plantillas existentes
            List<PlantillaBiometrica> existentes = plantillaRepository
                    .findByUsuarioAndModalidad(usuario, Modalidad.ROSTRO);

            // Desactivar plantillas anteriores
            for (PlantillaBiometrica old : existentes) {
                if (old.isActivo()) {
                    old.setActivo(false);
                    plantillaRepository.save(old);
                }
            }

            // Crear nueva plantilla
            PlantillaBiometrica plantilla = new PlantillaBiometrica();
            plantilla.setUsuario(usuario);
            plantilla.setModalidad(Modalidad.ROSTRO);
            plantilla.setPlantilla(plantillaBytes);
            plantilla.setProveedor("FaceRecognition");
            plantilla.setActivo(true);
            plantilla.setCreadoEn(java.time.OffsetDateTime.now());

            plantillaRepository.save(plantilla);
            System.out.println("[BiometricRegistration] Rostro registrado para: " + usuario.getNombreCompleto());

            if (sessionId != null && activeSessions.containsKey(sessionId)) {
                activeSessions.get(sessionId).rostroCompletado = true;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[BiometricRegistration] Error guardando rostro: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Guarda datos de voz
     */
    public boolean saveVoiceData(Usuario usuario, String voiceData, String sessionId) {
        try {
            // Permitir JSON (descriptor) o base64 de audio
            byte[] plantillaBytes;
            if (voiceData != null && voiceData.trim().startsWith("{")) {
                plantillaBytes = voiceData.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            } else {
                String payload = voiceData;
                if (payload != null && payload.startsWith("data:")) {
                    int comma = payload.indexOf(',');
                    payload = comma > 0 ? payload.substring(comma + 1) : payload;
                }
                plantillaBytes = Base64.getDecoder().decode(payload);
            }

            // Buscar plantillas existentes
            List<PlantillaBiometrica> existentes = plantillaRepository
                    .findByUsuarioAndModalidad(usuario, Modalidad.VOZ);

            // Desactivar plantillas anteriores
            for (PlantillaBiometrica old : existentes) {
                if (old.isActivo()) {
                    old.setActivo(false);
                    plantillaRepository.save(old);
                }
            }

            // Crear nueva plantilla
            PlantillaBiometrica plantilla = new PlantillaBiometrica();
            plantilla.setUsuario(usuario);
            plantilla.setModalidad(Modalidad.VOZ);
            plantilla.setPlantilla(plantillaBytes);
            plantilla.setProveedor("VoiceRecognition");
            plantilla.setActivo(true);
            plantilla.setCreadoEn(java.time.OffsetDateTime.now());

            plantillaRepository.save(plantilla);
            System.out.println("[BiometricRegistration] Voz registrada para: " + usuario.getNombreCompleto());

            if (sessionId != null && activeSessions.containsKey(sessionId)) {
                activeSessions.get(sessionId).vozCompletada = true;
            }

            return true;

        } catch (Exception e) {
            System.err.println("[BiometricRegistration] Error guardando voz: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtiene el estado del registro para un usuario
     */
    public Map<String, Object> getRegistrationStatus(Usuario usuario, String sessionId) {
        Map<String, Object> status = new HashMap<>();

        // Verificar qué plantillas tiene registradas en BD
        List<PlantillaBiometrica> plantillas = plantillaRepository.findByUsuario(usuario);

        boolean tieneHuella = plantillas.stream()
                .anyMatch(p -> p.getModalidad() == Modalidad.HUELLA && p.isActivo());
        boolean tieneRostro = plantillas.stream()
                .anyMatch(p -> p.getModalidad() == Modalidad.ROSTRO && p.isActivo());
        boolean tieneVoz = plantillas.stream()
                .anyMatch(p -> p.getModalidad() == Modalidad.VOZ && p.isActivo());

        status.put("userId", usuario.getId().toString());
        status.put("userName", usuario.getNombreCompleto());
        status.put("tieneHuella", tieneHuella);
        status.put("tieneRostro", tieneRostro);
        status.put("tieneVoz", tieneVoz);
        status.put("registroCompleto", tieneHuella && tieneRostro && tieneVoz);

        // Si hay sessionId, agregar información de la sesión
        if (sessionId != null && activeSessions.containsKey(sessionId)) {
            SessionData session = activeSessions.get(sessionId);
            status.put("sessionActive", true);
            status.put("huellaCompletada", session.huellaCompletada);
            status.put("rostroCompletado", session.rostroCompletado);
            status.put("vozCompletada", session.vozCompletada);
            status.put("sessionStartTime", session.startTime.toString());
        } else {
            status.put("sessionActive", false);
        }

        return status;
    }

    /**
     * Finaliza una sesión de registro
     */
    public void endSession(String sessionId) {
        activeSessions.remove(sessionId);
        System.out.println("[BiometricRegistration] Sesión finalizada: " + sessionId);
    }

    /**
     * Obtiene el usuario asociado a una sesión
     */
    public Usuario getUserFromSession(String sessionId) {
        SessionData session = activeSessions.get(sessionId);
        if (session == null) {
            return null;
        }

        try {
            UUID userId = UUID.fromString(session.userId);
            return usuarioRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            System.err.println("[BiometricRegistration] Error obteniendo usuario de sesión: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clase interna para almacenar datos de sesión
     */
    private static class SessionData {
        String userId;
        LocalDateTime startTime;
        boolean huellaCompletada;
        boolean rostroCompletado;
        boolean vozCompletada;
    }
}
