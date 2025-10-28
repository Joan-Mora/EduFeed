package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import okhttp3.*;

public class AccessApiClient {
    private final OkHttpClient http;
    private final HttpUrl baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String bearerToken;

    public AccessApiClient(String baseUrl, String bearerToken) {
        this.baseUrl = HttpUrl.parse(baseUrl);
        this.bearerToken = bearerToken;
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(5))
                .build();
    }

    public AccesoCheckResponseDto checkAccess(UUID usuarioId, String modalidad) throws IOException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("api/accesos/verificar")
                .build();
        String json = String.format("{\"usuarioId\":\"%s\",\"modalidad\":\"%s\"}", usuarioId, modalidad.toUpperCase());
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request.Builder rb = new Request.Builder().url(url).post(body);
        if (bearerToken != null && !bearerToken.isBlank()) {
            rb.header("Authorization", "Bearer " + bearerToken);
        }
        try (Response res = http.newCall(rb.build()).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("HTTP " + res.code() + " - " + (res.body()!=null?res.body().string():""));
            }
            String bodyStr = res.body() != null ? res.body().string() : "{}";
            return mapper.readValue(bodyStr, AccesoCheckResponseDto.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccesoCheckResponseDto {
        public Boolean permitido;
        public UsuarioDto usuario;
        public DerechoDto derecho;
        public String motivo;
        public String modalidad; // como texto para simplificar
        public OrientacionCajaDto orientacionCaja;
        public String timestamp;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsuarioDto {
        public String id;
        public String documento;
        public String nombreCompleto;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DerechoDto {
        public String id;
        public String tipo;
        public String vigenteHasta;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrientacionCajaDto {
        public String mensaje;
        public String ubicacionCaja;
        public String horarioAtencion;
        public String referencia;
        public String codigoQR; // puede ser data URL o texto
    }
}
