package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Duration;
import okhttp3.*;

public class AuthApiClient {
    private final OkHttpClient http;
    private final HttpUrl baseUrl;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public AuthApiClient(String baseUrl) {
        this.baseUrl = HttpUrl.parse(baseUrl);
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(5))
                .build();
    }

    public TokenPair login(String username, String password) throws IOException {
        HttpUrl url = baseUrl.newBuilder().addPathSegments("api/auth/login").build();
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(body).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                // Parsear mensaje de error amigable según el código HTTP
                String errorMsg = parseErrorMessage(res.code(), res.body(), url.encodedPath());
                throw new IOException(errorMsg);
            }
            String s = res.body() != null ? res.body().string() : "{}";
            LoginResponse r = mapper.readValue(s, LoginResponse.class);
            return new TokenPair(r.tokenType, r.accessToken, r.refreshToken);
        }
    }

    private String parseErrorMessage(int statusCode, ResponseBody body, String path) {
        String bodyStr = "";
        try {
            if (body != null) {
                bodyStr = body.string(); // consumir el body una sola vez
            }
        } catch (Exception ignored) {
        }

        String lower = bodyStr.toLowerCase();
        // Heurísticas para detectar credenciales inválidas incluso si el backend
        // devuelve 5xx
        boolean looksAuthFailure = lower.contains("bad credentials") ||
                (lower.contains("invalid") && (lower.contains("password") || lower.contains("usuario"))) ||
                (lower.contains("credencial") && lower.contains("incorrect")) ||
                (lower.contains("contraseña") && (lower.contains("incorrect") || lower.contains("inval"))) ||
                (lower.contains("usuario") && lower.contains("incorrect")) ||
                lower.contains("authentication failed") ||
                lower.contains("auth failed") ||
                lower.contains("login failed") ||
                lower.contains("wrong password") ||
                // Algunos servicios incluyen status interno en el JSON
                lower.contains("\"status\":401") || lower.contains("\"status\":403");

        // Señales de servicio caído/infra
        boolean looksServiceDown = lower.contains("service unavailable") ||
                lower.contains("mantenimiento") ||
                lower.contains("maintenance") ||
                lower.contains("timeout") ||
                lower.contains("timed out") ||
                lower.contains("conexion") || lower.contains("conexión") || lower.contains("connection") ||
                lower.contains("refused") || lower.contains("rechazada") ||
                lower.contains("database") || lower.contains("db error") || lower.contains("internal server error");

        // Si el código es típico de auth o el contenido sugiere fallo de auth → mensaje
        // de credenciales
        if (statusCode == 400 || statusCode == 401 || statusCode == 403 || looksAuthFailure) {
            return "Usuario o Contraseña Incorrectos";
        }

        if (statusCode == 404) {
            return "Servidor no disponible. Verifique la conexión";
        }

        if (statusCode == 500 || statusCode == 502 || statusCode == 503) {
            // Reconfirmar heurística por si el backend manda 5xx para auth inválida
            if (looksAuthFailure) {
                return "Usuario o Contraseña Incorrectos";
            }
            // Para el endpoint de login, es preferible mostrar credenciales inválidas
            // salvo que detectemos pistas claras de caída del servicio
            if (path != null && path.contains("/auth/login") && !looksServiceDown) {
                return "Usuario o Contraseña Incorrectos";
            }
            return "Error en el servidor. Intente nuevamente más tarde";
        }

        return "Error de conexión. Verifique su red";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginResponse {
        public String tokenType;
        public String accessToken;
        public String refreshToken;
    }

    public record TokenPair(String tokenType, String accessToken, String refreshToken) {
    }
}
