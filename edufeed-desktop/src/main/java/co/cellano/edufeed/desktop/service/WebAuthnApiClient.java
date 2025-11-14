package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import okhttp3.*;

/** Cliente HTTP para flujo WebAuthn (autenticación por teléfono). */
public class WebAuthnApiClient {
    private final OkHttpClient http;
    private final HttpUrl baseUrl;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final String bearerToken;

    public WebAuthnApiClient(String baseUrl, String bearerToken) {
        this.baseUrl = HttpUrl.parse(baseUrl);
        this.bearerToken = bearerToken;
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(5))
                .build();
    }

    public IniciarResponse iniciarAutenticacion(String usuarioDocumento) throws IOException {
        HttpUrl url = baseUrl.newBuilder().addPathSegments("api/webauthn/autenticacion/iniciar").build();
        String json = String.format("{\"usuarioDocumento\":\"%s\"}", usuarioDocumento);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request.Builder rb = new Request.Builder().url(url).post(body);
        addAuth(rb);
        try (Response res = http.newCall(rb.build()).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + bodyStr(res));
            return mapper.readValue(bodyStr(res), IniciarResponse.class);
        }
    }

    public IniciarResponse iniciarRegistro(String usuarioDocumento) throws IOException {
        HttpUrl url = baseUrl.newBuilder().addPathSegments("api/webauthn/registro/iniciar").build();
        String json = String.format("{\"usuarioDocumento\":\"%s\"}", usuarioDocumento);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request.Builder rb = new Request.Builder().url(url).post(body);
        addAuth(rb);
        try (Response res = http.newCall(rb.build()).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + bodyStr(res));
            return mapper.readValue(bodyStr(res), IniciarResponse.class);
        }
    }

    public QrData obtenerQr(UUID sesionId) throws IOException {
        HttpUrl url = baseUrl.newBuilder().addPathSegments("api/webauthn/qr/").addPathSegment(sesionId.toString())
                .build();
        Request.Builder rb = new Request.Builder().url(url).get();
        addAuth(rb);
        try (Response res = http.newCall(rb.build()).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + bodyStr(res));
            Map<?, ?> m = mapper.readValue(bodyStr(res), Map.class);
            QrData q = new QrData();
            q.sesionId = sesionId.toString();
            Object urlObj = m.get("url");
            q.url = urlObj != null ? urlObj.toString() : "";
            Object msgObj = m.get("mensaje");
            q.mensaje = msgObj != null ? msgObj.toString() : "";
            return q;
        }
    }

    public EstadoSesion obtenerEstado(UUID sesionId) throws IOException {
        HttpUrl url = baseUrl.newBuilder().addPathSegments("api/webauthn/sesion/").addPathSegment(sesionId.toString())
                .build();
        Request.Builder rb = new Request.Builder().url(url).get();
        addAuth(rb);
        try (Response res = http.newCall(rb.build()).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + bodyStr(res));
            return mapper.readValue(bodyStr(res), EstadoSesion.class);
        }
    }

    private void addAuth(Request.Builder rb) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            rb.header("Authorization", "Bearer " + bearerToken);
        }
    }

    private static String bodyStr(Response res) throws IOException {
        return res.body() != null ? res.body().string() : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IniciarResponse {
        public String sesionId;
        public String challenge;
        public String usuarioDocumento;
        public String qrUrl; // /api/webauthn/qr/{sesionId}
    }

    public static class QrData {
        public String sesionId;
        public String url;
        public String mensaje;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EstadoSesion {
        public String sesionId;
        public String estado; // PENDIENTE | COMPLETADA | EXPIRADA | FALLIDA
        public String tipo; // AUTENTICACION | REGISTRO
        public Boolean exito;
        public String mensaje;
    }
}
