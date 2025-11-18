package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para manejar autenticación biométrica.
 * 
 * Incluye:
 * - Verificación de huella, FaceID y voz
 * - Notificación a desktop vía WebSocket
 * - Manejo de estado de autenticación
 */
@Service
public class BiometricAuthService {

    @Autowired
    private PlantillaBiometricaRepository plantillaBiometricaRepository;

    // Cache de autenticaciones en progreso
    private final Map<String, AuthSession> authSessions = new ConcurrentHashMap<>();

    // Desde ahora, en lugar de listas hardcodeadas usamos la base de datos de
    // plantillas activas
    private boolean hasModality(Usuario user, Modalidad modalidad) {
        if (user == null || user.getId() == null)
            return false;
        return plantillaBiometricaRepository.findByUsuarioIdAndActivoTrue(user.getId())
                .stream()
                .anyMatch(p -> p.getModalidad() == modalidad);
    }

    /**
     * Verifica huella digital
     */
    public boolean verifyFingerprint(Usuario user, String fingerprintData) {
        if (!hasModality(user, Modalidad.HUELLA)) {
            System.out.println("[BIO-AUTH][FINGERPRINT] Usuario sin plantilla activa");
            return false;
        }

        if (fingerprintData == null || fingerprintData.isBlank()) {
            System.out.println("[BIO-AUTH][FINGERPRINT] Datos vacíos");
            return false;
        }

        // Cuando viene de WebAuthn el cliente suele enviar un JSON (id, rawId,
        // response,...)
        if (fingerprintData.trim().startsWith("{")) {
            // Validación mínima: contener campos clave
            boolean ok = fingerprintData.contains("\"id\"") && fingerprintData.contains("\"response\"")
                    && fingerprintData.contains("\"clientDataJSON\"");
            System.out.println("[BIO-AUTH][FINGERPRINT] Entrada JSON detectada. Campos requeridos presentes=" + ok);
            return ok; // Hasta integrar verificación de firma real.
        }

        // Si llega base64 plano (flujo simplificado anterior)
        try {
            byte[] data = Base64.getDecoder().decode(fingerprintData);
            if (data.length < 100) { // umbral mínimo arbitrario para descartar basura muy corta
                System.out.println("[BIO-AUTH][FINGERPRINT] Muestra demasiado corta " + data.length + " bytes");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("[BIO-AUTH][FINGERPRINT] Fallo decodificando base64: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica FaceID
     */
    public boolean verifyFaceId(Usuario user, String faceData) {
        if (!hasModality(user, Modalidad.ROSTRO)) {
            System.out.println("[BIO-AUTH][FACE] Usuario sin plantilla activa");
            return false;
        }
        if (faceData == null || faceData.isBlank()) {
            System.out.println("[BIO-AUTH][FACE] Datos vacíos");
            return false;
        }
        // Si llega JSON con descriptor, comparar contra plantilla con descriptor
        if (faceData.trim().startsWith("{")) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(faceData);
                if (node.has("descriptor") && node.get("descriptor").isArray()) {
                    double[] probe = toDoubleArray(node.get("descriptor"));
                    var plantillas = plantillaBiometricaRepository.findByUsuarioIdAndActivoTrue(user.getId())
                            .stream().filter(p -> p.getModalidad() == Modalidad.ROSTRO).toList();
                    for (var p : plantillas) {
                        byte[] bytes = p.getPlantilla();
                        if (bytes != null && bytes.length > 0 && bytes[0] == '{') {
                            var stored = mapper.readTree(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                            if (stored.has("descriptor")) {
                                double[] ref = toDoubleArray(stored.get("descriptor"));
                                double dist = l2(probe, ref);
                                System.out.println("[BIO-AUTH][FACE] L2 distance=" + dist);
                                if (dist < 0.6)
                                    return true; // umbral típico para 128-D
                            }
                        }
                    }
                    System.out.println("[BIO-AUTH][FACE] No hay plantilla con descriptor para comparar");
                    return false;
                }
            } catch (Exception e) {
                System.out.println("[BIO-AUTH][FACE] Error analizando descriptor JSON: " + e.getMessage());
                return false;
            }
        }
        // Fallback: imagen base64 con validaciones básicas (no identidad)
        String payload = faceData;
        if (payload.startsWith("data:")) {
            int idx = payload.indexOf(',');
            if (idx > 0)
                payload = payload.substring(idx + 1);
        }
        try {
            byte[] data = Base64.getDecoder().decode(payload);
            if (data.length < 15_000)
                return false;
            int varied = 0;
            for (int i = 0; i < data.length; i += 500) {
                int v = data[i] & 0xFF;
                if (v != 0 && v != 255)
                    varied++;
            }
            return varied >= 10;
        } catch (Exception e) {
            System.out.println("[BIO-AUTH][FACE] Fallo decodificando base64: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica voz
     */
    public boolean verifyVoice(Usuario user, String voiceData) {
        if (!hasModality(user, Modalidad.VOZ)) {
            System.out.println("[BIO-AUTH][VOICE] Usuario sin plantilla activa");
            return false;
        }
        if (voiceData == null || voiceData.isBlank()) {
            System.out.println("[BIO-AUTH][VOICE] Datos vacíos");
            return false;
        }
        // Si llega JSON con features (MFCC/embedding), comparar contra plantilla
        if (voiceData.trim().startsWith("{")) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(voiceData);
                if (node.has("features") && node.get("features").isArray()) {
                    double[] probe = toDoubleArray(node.get("features"));
                    var plantillas = plantillaBiometricaRepository.findByUsuarioIdAndActivoTrue(user.getId())
                            .stream().filter(p -> p.getModalidad() == Modalidad.VOZ).toList();
                    for (var p : plantillas) {
                        byte[] bytes = p.getPlantilla();
                        if (bytes != null && bytes.length > 0 && bytes[0] == '{') {
                            var stored = mapper.readTree(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                            if (stored.has("features")) {
                                double[] ref = toDoubleArray(stored.get("features"));
                                double sim = cosine(probe, ref);
                                System.out.println("[BIO-AUTH][VOICE] Cosine similarity=" + sim);
                                if (sim > 0.85)
                                    return true; // umbral experimental
                            }
                        }
                    }
                    System.out.println("[BIO-AUTH][VOICE] No hay plantilla con features para comparar");
                    return false;
                }
            } catch (Exception e) {
                System.out.println("[BIO-AUTH][VOICE] Error analizando JSON de features: " + e.getMessage());
                return false;
            }
        }
        String payload = voiceData;
        if (payload.startsWith("data:")) {
            int idx = payload.indexOf(',');
            if (idx > 0)
                payload = payload.substring(idx + 1);
        }
        try {
            byte[] data = Base64.getDecoder().decode(payload);
            if (data.length < 12_000) { // ~ ruido muy corto (< ~1-1.5s en webm comprimido)
                System.out.println("[BIO-AUTH][VOICE] Audio demasiado corto " + data.length + " bytes");
                return false;
            }
            // Heurística de energía: varianza de bytes (no PCM real)
            long sum = 0;
            for (int i = 0; i < data.length; i += 3)
                sum += (data[i] & 0xFF);
            double mean = (double) sum / (data.length / 3.0);
            double varSum = 0;
            for (int i = 0; i < data.length; i += 3) {
                double v = (data[i] & 0xFF) - mean;
                varSum += v * v;
            }
            double variance = varSum / (data.length / 3.0);
            if (variance < 1500) { // umbral experimental
                System.out.println("[BIO-AUTH][VOICE] Varianza baja (" + variance + ") -> posible silencio");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("[BIO-AUTH][VOICE] Fallo decodificando base64: " + e.getMessage());
            return false;
        }
    }

    // Utilidades
    private static double[] toDoubleArray(com.fasterxml.jackson.databind.JsonNode arr) {
        double[] v = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++)
            v[i] = arr.get(i).asDouble();
        return v;
    }

    private static double l2(double[] a, double[] b) {
        if (a.length != b.length)
            return Double.POSITIVE_INFINITY;
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
    }

    private static double cosine(double[] a, double[] b) {
        if (a.length != b.length)
            return -1;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0)
            return -1;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Verifica si el usuario tiene un método biométrico registrado
     */
    public boolean hasFingerprint(Usuario user) {
        return hasModality(user, Modalidad.HUELLA);
    }

    public boolean hasFaceId(Usuario user) {
        return hasModality(user, Modalidad.ROSTRO);
    }

    public boolean hasVoice(Usuario user) {
        return hasModality(user, Modalidad.VOZ);
    }

    /**
     * Notifica al desktop que la autenticación fue exitosa
     */
    public void notifyDesktop(String userId, Usuario user) {
        // Crear sesión de autenticación
        AuthSession session = new AuthSession();
        session.setUserId(userId);
        session.setAuthenticated(true);
        session.setUserData(getUserPaymentData(user));

        authSessions.put(userId, session);

        // El desktop hará polling para obtener el estado
        System.out.println("✓ Autenticación exitosa para usuario: " + userId);
    }

    /**
     * Obtiene datos de pagos del usuario
     */
    public Map<String, Object> getUserPaymentData(Usuario user) {
        Map<String, Object> data = new HashMap<>();

        // Usar campos reales del modelo Usuario
        data.put("nombre", user.getNombreCompleto());
        data.put("cedula", user.getDocumento());

        // Sueldo simulado (en producción vendría de otra tabla/cálculo)
        data.put("sueldo", "2500000");

        // Contar pagos por estado - SIMULADO por ahora
        // En producción, usar: pagoRepository.countByUsuarioIdAndEstado(...)
        data.put("pagosPendientes", "3");
        data.put("pagosRealizados", "12");
        data.put("pagosProgramados", "2");

        return data;
    }

    /**
     * Obtiene el estado de autenticación (para polling desde desktop)
     */
    public Map<String, Object> getAuthStatus(String userId) {
        Map<String, Object> status = new HashMap<>();

        AuthSession session = authSessions.get(userId);

        if (session != null && session.isAuthenticated()) {
            status.put("authenticated", true);
            status.put("userData", session.getUserData());

            // Limpiar sesión después de obtenerla
            authSessions.remove(userId);
        } else {
            status.put("authenticated", false);
        }

        return status;
    }

    /**
     * Clase interna para manejar sesiones de autenticación
     */
    private static class AuthSession {
        private String userId;
        private boolean authenticated;
        private Map<String, Object> userData;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public Map<String, Object> getUserData() {
            return userData;
        }

        public void setUserData(Map<String, Object> userData) {
            this.userData = userData;
        }
    }
}
