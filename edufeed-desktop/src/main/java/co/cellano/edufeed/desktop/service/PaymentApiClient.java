package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/**
 * Cliente HTTP para pagos y búsqueda de usuarios.
 * Usa endpoints del backend:
 * - POST /api/pagos
 * - PUT /api/pagos/{id}/aprobar
 * - GET /api/usuarios/buscar/documento/{documento}
 * - GET /api/usuarios/buscar/nombre?q=
 */
public class PaymentApiClient {
    private final String baseUrl;
    private final String bearer;
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public PaymentApiClient(String baseUrl, String bearer) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
        this.bearer = bearer;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    private Request.Builder authJson(Request.Builder b) {
        return b.addHeader("Authorization", "Bearer "+ bearer)
                .addHeader("Content-Type", "application/json");
    }

    public Optional<UsuarioDto> buscarUsuarioPorDocumento(String documento) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/buscar/documento/" + documento);
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() == 404) return Optional.empty();
            if (!res.isSuccessful()) throw new IOException("HTTP "+res.code()+": "+ (res.body()!=null?res.body().string():""));
            return Optional.of(mapper.readValue(res.body().byteStream(), UsuarioDto.class));
        }
    }

    public List<UsuarioDto> buscarUsuariosPorNombre(String query) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/buscar/nombre").newBuilder()
                .addQueryParameter("q", query).build();
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("HTTP "+res.code()+": "+ (res.body()!=null?res.body().string():""));
            return mapper.readValue(res.body().byteStream(), new TypeReference<List<UsuarioDto>>(){});
        }
    }

    public PagoDto crearPago(CreatePagoRequest reqBody) throws IOException {
        String json = mapper.writeValueAsString(reqBody);
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/pagos").post(RequestBody.create(json, MediaType.parse("application/json"))))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("HTTP "+res.code()+": "+ (res.body()!=null?res.body().string():""));
            return mapper.readValue(res.body().byteStream(), PagoDto.class);
        }
    }

    public PagoDto aprobarPago(UUID pagoId) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/pagos/"+ pagoId + "/aprobar")
                .put(RequestBody.create(new byte[0])))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("HTTP "+res.code()+": "+ (res.body()!=null?res.body().string():""));
            return mapper.readValue(res.body().byteStream(), PagoDto.class);
        }
    }

    // ---- DTOs Cliente (compatibles con backend) ----

    public enum TipoPago { DIARIO, MENSUAL, PAQUETE }
    public enum EstadoPago { PENDIENTE, APROBADO, RECHAZADO }

    public static final class UsuarioDto {
        public String id;
        public String documento;
        public String nombreCompleto;
        public String email;
        public String telefono;
        public Boolean activo;
        @Override public String toString(){ return nombreCompleto + " ("+documento+")"; }
    }

    public static final class PagoDto {
        public String id;
        public String usuarioId;
        public BigDecimal monto;
        public TipoPago tipoPago;
        public EstadoPago estadoPago;
        public OffsetDateTime creadoEn;
        public OffsetDateTime vigenteDesde;
        public OffsetDateTime vigenteHasta;
        public String metodoPago;
        public String referenciaExterna;
        public String cajero;
        public String metadatos;
        public Integer diasPaquete;
    }

    public static final class CreatePagoRequest {
        public String usuarioId;
        public BigDecimal monto;
        public TipoPago tipoPago;
        public String metodoPago; // EFECTIVO, TARJETA, TRANSFERENCIA, POS
        public String referenciaExterna;
        public Integer diasPaquete; // solo para PAQUETE
        public String cajero; // opcional
        public String metadatos; // opcional

        public CreatePagoRequest() {}
        public CreatePagoRequest(String usuarioId, BigDecimal monto, TipoPago tipoPago,
                                 String metodoPago, String referenciaExterna, Integer diasPaquete,
                                 String cajero, String metadatos) {
            this.usuarioId = usuarioId; this.monto = monto; this.tipoPago = tipoPago;
            this.metodoPago = metodoPago; this.referenciaExterna = referenciaExterna;
            this.diasPaquete = diasPaquete; this.cajero = cajero; this.metadatos = metadatos;
        }
    }
}
