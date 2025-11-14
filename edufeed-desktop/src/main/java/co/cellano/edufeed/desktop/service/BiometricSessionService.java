package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para gestionar sesiones de registro biométrico con el backend.
 */
public class BiometricSessionService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public BiometricSessionService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Inicia una nueva sesión de registro biométrico.
     *
     * @param userId ID del usuario
     * @param token  Token de autenticación
     * @return ID de la sesión creada
     * @throws Exception si hay error en la comunicación
     */
    public String startSession(String userId, String token) throws Exception {
        String endpoint = baseUrl + "/api/biometric/register/session/start";

        // Crear JSON del request
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("userId", userId);
        requestBody.put("token", token);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String body = response.body();
            throw new Exception(
                    "HTTP " + response.statusCode() + " al iniciar sesión: " + (body != null ? body : "(sin cuerpo)"));
        }

        // Parsear respuesta
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

        if (responseMap.containsKey("error")) {
            throw new Exception("Error: " + String.valueOf(responseMap.get("error")));
        }

        return (String) responseMap.get("sessionId");
    }

    /**
     * Obtiene el estado actual del registro biométrico.
     *
     * @param userId    ID del usuario
     * @param sessionId ID de la sesión (opcional)
     * @return Mapa con el estado del registro
     * @throws Exception si hay error en la comunicación
     */
    public RegistrationStatus getStatus(String userId, String sessionId) throws Exception {
        String endpoint = baseUrl + "/api/biometric/register/status/" + userId;

        if (sessionId != null && !sessionId.isEmpty()) {
            endpoint += "?sessionId=" + sessionId;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Error al obtener estado: " + response.body());
        }

        // Parsear respuesta
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

        RegistrationStatus status = new RegistrationStatus();
        status.userId = (String) responseMap.get("userId");
        status.userName = (String) responseMap.get("userName");
        status.tieneHuella = (Boolean) responseMap.getOrDefault("tieneHuella", false);
        status.tieneRostro = (Boolean) responseMap.getOrDefault("tieneRostro", false);
        status.tieneVoz = (Boolean) responseMap.getOrDefault("tieneVoz", false);
        status.registroCompleto = (Boolean) responseMap.getOrDefault("registroCompleto", false);
        status.sessionActive = (Boolean) responseMap.getOrDefault("sessionActive", false);

        if (status.sessionActive) {
            status.huellaCompletada = (Boolean) responseMap.getOrDefault("huellaCompletada", false);
            status.rostroCompletado = (Boolean) responseMap.getOrDefault("rostroCompletado", false);
            status.vozCompletada = (Boolean) responseMap.getOrDefault("vozCompletada", false);
        }

        return status;
    }

    /**
     * Genera un token único para la sesión.
     *
     * @return Token generado
     */
    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Clase para representar el estado del registro.
     */
    public static class RegistrationStatus {
        public String userId;
        public String userName;
        public boolean tieneHuella;
        public boolean tieneRostro;
        public boolean tieneVoz;
        public boolean registroCompleto;
        public boolean sessionActive;
        public boolean huellaCompletada;
        public boolean rostroCompletado;
        public boolean vozCompletada;

        public int getCompletedCount() {
            int count = 0;
            if (tieneHuella)
                count++;
            if (tieneRostro)
                count++;
            if (tieneVoz)
                count++;
            return count;
        }

        public int getSessionCompletedCount() {
            int count = 0;
            if (huellaCompletada)
                count++;
            if (rostroCompletado)
                count++;
            if (vozCompletada)
                count++;
            return count;
        }

        @Override
        public String toString() {
            return String.format("RegistrationStatus{userId='%s', userName='%s', completo=%d/3, " +
                    "huella=%s, rostro=%s, voz=%s}",
                    userId, userName, getCompletedCount(),
                    tieneHuella, tieneRostro, tieneVoz);
        }
    }
}
