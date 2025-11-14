package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public PaymentApiClient(String baseUrl, String bearer) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bearer = bearer;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    private Request.Builder authJson(Request.Builder b) {
        return b.addHeader("Authorization", "Bearer " + bearer)
                .addHeader("Content-Type", "application/json");
    }

    public Optional<UsuarioDto> buscarUsuarioPorDocumento(String documento) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/buscar/documento/" + documento);
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() == 404)
                return Optional.empty();
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            return Optional.of(mapper.readValue(res.body().byteStream(), UsuarioDto.class));
        }
    }

    /**
     * Obtiene un usuario por ID.
     */
    public Optional<UsuarioDto> getUsuario(UUID id) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/" + id);
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() == 404)
                return Optional.empty();
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            return Optional.of(mapper.readValue(res.body().byteStream(), UsuarioDto.class));
        }
    }

    public List<UsuarioDto> buscarUsuariosPorNombre(String query) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/buscar/nombre").newBuilder()
                .addQueryParameter("q", query).build();
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            return mapper.readValue(res.body().byteStream(), new TypeReference<List<UsuarioDto>>() {
            });
        }
    }

    public PagoDto crearPago(CreatePagoRequest reqBody) throws IOException {
        String json = mapper.writeValueAsString(reqBody);
        System.out.println("[PaymentApiClient] Enviando POST /api/pagos JSON=" + json);
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/pagos").post(RequestBody.create(json, MediaType.parse("application/json"))))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            return mapper.readValue(res.body().byteStream(), PagoDto.class);
        }
    }

    public PagoDto aprobarPago(UUID pagoId) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/pagos/" + pagoId + "/aprobar")
                .put(RequestBody.create(new byte[0])))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            return mapper.readValue(res.body().byteStream(), PagoDto.class);
        }
    }

    /**
     * Obtiene un pago por ID.
     */
    public Optional<PagoDto> getPago(UUID pagoId) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/pagos/" + pagoId);
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() == 404)
                return Optional.empty();
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            return Optional.of(mapper.readValue(res.body().byteStream(), PagoDto.class));
        }
    }

    /**
     * Lista todos los pagos con información enriquecida del usuario.
     * TODO: Por ahora retorna lista vacía hasta que el backend implemente este
     * endpoint.
     */
    public List<PagoEnriquecidoDto> listarPagos() throws IOException {
        // 1) Obtener pagos (PagoDto) del backend
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/pagos");
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() == 404) {
                return java.util.Collections.emptyList();
            }
            if (!res.isSuccessful()) {
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            }
            List<PagoDto> pagos = mapper.readValue(res.body().byteStream(), new TypeReference<List<PagoDto>>() {
            });

            // 2) Enriquecer con datos de usuario cuando sea posible
            java.util.ArrayList<PagoEnriquecidoDto> out = new java.util.ArrayList<>();
            for (PagoDto p : pagos) {
                PagoEnriquecidoDto e = new PagoEnriquecidoDto();
                e.id = p.id;
                e.usuarioId = p.usuarioId;
                e.monto = p.monto;
                e.tipoPago = p.tipoPago;
                e.estadoPago = p.estadoPago;
                e.creadoEn = p.creadoEn;
                e.vigenteDesde = p.vigenteDesde;
                e.vigenteHasta = p.vigenteHasta;
                e.metodoPago = p.metodoPago;
                e.referenciaExterna = p.referenciaExterna;
                e.cajero = p.cajero;
                e.metadatos = p.metadatos;
                e.diasPaquete = p.diasPaquete;

                // Interpretar devoluciones: si el backend dejó RECHAZADO pero los metadatos
                // tienen "motivo_devolucion", mostramos como REVERTIDO en el cliente.
                try {
                    if (e.estadoPago == EstadoPago.RECHAZADO && e.metadatos != null
                            && e.metadatos.contains("\"motivo_devolucion\"")) {
                        e.estadoPago = EstadoPago.REVERTIDO;
                    }
                } catch (Exception ignore) {
                }

                // Intentar resolver usuario
                try {
                    if (p.usuarioId != null && !p.usuarioId.isBlank()) {
                        Optional<UsuarioDto> u = getUsuario(UUID.fromString(p.usuarioId));
                        if (u.isPresent()) {
                            e.usuarioNombre = u.get().nombreCompleto;
                            e.usuarioDocumento = u.get().documento;
                        }
                    }
                } catch (Exception ignore) {
                    // Si falla, dejamos los campos de usuario como null
                }

                out.add(e);
            }
            return out;
        }
    }

    /**
     * Revierte/cancela un pago.
     * TODO: Por ahora lanza excepción hasta que el backend implemente este
     * endpoint.
     */
    public void revertirPago(UUID pagoId) throws IOException {
        // Usar endpoint dedicado de reversión/devolución
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/pagos/" + pagoId + "/revertir")
                .put(RequestBody.create(new byte[0]))).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            }
        }
    }

    /**
     * Actualiza solo el campo metadatos de un pago existente.
     * PUT /api/pagos/{id} con cuerpo parcial { "metadatos": "..." }
     */
    public PagoDto actualizarMetadatosPago(UUID pagoId, String metadatos) throws IOException {
        // La API de update valida campos obligatorios del DTO; obtener el pago actual
        PagoDto actual = getPago(pagoId).orElseThrow(() -> new IOException("Pago no encontrado: " + pagoId));

        // Construir cuerpo con campos requeridos + metadatos actualizado
        // Nota: el servicio update ignorará usuarioId/monto/tipoPago y solo tomará
        // metadatos
        String json = mapper.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {
            {
                put("usuarioId", actual.usuarioId);
                put("monto", actual.monto);
                put("tipoPago", actual.tipoPago);
                put("metadatos", metadatos);
            }
        });

        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/pagos/" + pagoId)
                .put(RequestBody.create(json, MediaType.parse("application/json"))))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("HTTP " + res.code() + ": " + (res.body() != null ? res.body().string() : ""));
            }
            return mapper.readValue(res.body().byteStream(), PagoDto.class);
        }
    }

    /**
     * Actualiza el comentario/metadatos de un pago.
     * TODO: Por ahora lanza excepción hasta que el backend implemente este
     * endpoint.
     */
    public void actualizarComentario(UUID pagoId, String comentario) throws IOException {
        // TODO: Cuando el backend tenga el endpoint PATCH /api/pagos/{id}/comentario
        throw new IOException("Funcionalidad de actualizar comentario aún no implementada en el backend");
    }

    /**
     * Busca facturas por referencia exacta.
     * Retorna lista con un elemento si encuentra, vacía si no.
     */
    public List<PagoEnriquecidoDto> buscarFacturaPorReferencia(String referencia) throws IOException {
        // Estrategia temporal: listar todos y filtrar en cliente
        // TODO: Implementar endpoint GET /api/pagos/buscar/referencia/{ref} en backend
        List<PagoEnriquecidoDto> todos = listarPagos();
        return todos.stream()
                .filter(p -> p.referenciaExterna != null && p.referenciaExterna.equalsIgnoreCase(referencia))
                .toList();
    }

    /**
     * Busca facturas por documento de usuario.
     */
    public List<PagoEnriquecidoDto> buscarFacturasPorDocumento(String documento) throws IOException {
        // Estrategia temporal: listar todos y filtrar
        // TODO: Implementar endpoint GET /api/pagos/buscar/documento/{doc} en backend
        List<PagoEnriquecidoDto> todos = listarPagos();
        return todos.stream()
                .filter(p -> p.usuarioDocumento != null && p.usuarioDocumento.contains(documento))
                .toList();
    }

    /**
     * Busca facturas por nombre de usuario (búsqueda parcial).
     */
    public List<PagoEnriquecidoDto> buscarFacturasPorNombre(String nombre) throws IOException {
        // Estrategia temporal: listar todos y filtrar
        // TODO: Implementar endpoint GET /api/pagos/buscar/nombre?q={nombre} en backend
        List<PagoEnriquecidoDto> todos = listarPagos();
        String nombreLower = nombre.toLowerCase();
        return todos.stream()
                .filter(p -> p.usuarioNombre != null && p.usuarioNombre.toLowerCase().contains(nombreLower))
                .toList();
    }

    // ---- DTOs Cliente (compatibles con backend) ----

    public enum TipoPago {
        DIARIO, MENSUAL, PAQUETE
    }

    public enum EstadoPago {
        PENDIENTE, APROBADO, RECHAZADO, REVERTIDO
    }

    public static final class UsuarioDto {
        public String id;
        public String documento;
        public String nombreCompleto;
        public String email;
        public String telefono;
        public Boolean activo;

        @Override
        public String toString() {
            return nombreCompleto + " (" + documento + ")";
        }
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

    /**
     * DTO enriquecido con información del usuario para mostrar en tablas.
     */
    public static final class PagoEnriquecidoDto {
        public String id;
        public String usuarioId;
        public String usuarioNombre;
        public String usuarioDocumento;
        public BigDecimal monto;
        public TipoPago tipoPago;
        public EstadoPago estadoPago;
        public OffsetDateTime creadoEn;
        public OffsetDateTime vigenteDesde;
        public OffsetDateTime vigenteHasta;
        public String metodoPago;
        public String referenciaExterna;
        public String cajero;
        public String metadatos; // usado como comentario
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

        public CreatePagoRequest() {
        }

        public CreatePagoRequest(String usuarioId, BigDecimal monto, TipoPago tipoPago,
                String metodoPago, String referenciaExterna, Integer diasPaquete,
                String cajero, String metadatos) {
            this.usuarioId = usuarioId;
            this.monto = monto;
            this.tipoPago = tipoPago;
            this.metodoPago = metodoPago;
            this.referenciaExterna = referenciaExterna;
            this.diasPaquete = diasPaquete;
            this.cajero = cajero;
            this.metadatos = metadatos;
        }

        // Getters para Jackson serialization
        public String getUsuarioId() {
            return usuarioId;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public TipoPago getTipoPago() {
            return tipoPago;
        }

        public String getMetodoPago() {
            return metodoPago;
        }

        public String getReferenciaExterna() {
            return referenciaExterna;
        }

        public Integer getDiasPaquete() {
            return diasPaquete;
        }

        public String getCajero() {
            return cajero;
        }

        public String getMetadatos() {
            return metadatos;
        }
    }
}
