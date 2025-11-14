package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import okhttp3.*;

/** Cliente HTTP para endpoints de reportes (ADMIN/AUDITOR) */
public class ReportApiClient {
    private final String baseUrl;
    private final String bearer;
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ReportApiClient(String baseUrl, String bearer) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bearer = bearer;
        this.http = new OkHttpClient();
    }

    private Request.Builder authJson(Request.Builder b) {
        return b.addHeader("Authorization", "Bearer " + bearer)
                .addHeader("Accept", "application/json");
    }

    public List<IngresosDiariosItem> ingresos(LocalDate desde, LocalDate hasta) throws IOException {
        HttpUrl.Builder b = HttpUrl.parse(baseUrl + "/api/reportes/ingresos").newBuilder();
        if (desde != null)
            b.addQueryParameter("desde", desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).toString());
        if (hasta != null)
            b.addQueryParameter("hasta", hasta.plusDays(1).atStartOfDay().minusNanos(1)
                    .atOffset(OffsetDateTime.now().getOffset()).toString());
        Request req = authJson(new Request.Builder().url(b.build()).get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            return mapper.readValue(res.body().byteStream(), new TypeReference<List<IngresosDiariosItem>>() {
            });
        }
    }

    public BigDecimal resumenIngresos(LocalDate desde, LocalDate hasta) throws IOException {
        HttpUrl.Builder b = HttpUrl.parse(baseUrl + "/api/reportes/ingresos/resumen").newBuilder();
        if (desde != null)
            b.addQueryParameter("desde", desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).toString());
        if (hasta != null)
            b.addQueryParameter("hasta", hasta.plusDays(1).atStartOfDay().minusNanos(1)
                    .atOffset(OffsetDateTime.now().getOffset()).toString());
        Request req = authJson(new Request.Builder().url(b.build()).get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String s = res.body() != null ? res.body().string() : "0";
            return new BigDecimal(s);
        }
    }

    public List<AsistenciasDiariasItem> asistencias(LocalDate desde, LocalDate hasta) throws IOException {
        HttpUrl.Builder b = HttpUrl.parse(baseUrl + "/api/reportes/asistencias").newBuilder();
        if (desde != null)
            b.addQueryParameter("desde", desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).toString());
        if (hasta != null)
            b.addQueryParameter("hasta", hasta.plusDays(1).atStartOfDay().minusNanos(1)
                    .atOffset(OffsetDateTime.now().getOffset()).toString());
        Request req = authJson(new Request.Builder().url(b.build()).get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            return mapper.readValue(res.body().byteStream(), new TypeReference<List<AsistenciasDiariasItem>>() {
            });
        }
    }

    public List<RechazosDiariosItem> rechazos(LocalDate desde, LocalDate hasta) throws IOException {
        HttpUrl.Builder b = HttpUrl.parse(baseUrl + "/api/reportes/rechazos").newBuilder();
        if (desde != null)
            b.addQueryParameter("desde", desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).toString());
        if (hasta != null)
            b.addQueryParameter("hasta", hasta.plusDays(1).atStartOfDay().minusNanos(1)
                    .atOffset(OffsetDateTime.now().getOffset()).toString());
        Request req = authJson(new Request.Builder().url(b.build()).get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            return mapper.readValue(res.body().byteStream(), new TypeReference<List<RechazosDiariosItem>>() {
            });
        }
    }

    public List<DerechoActivoItem> derechosActivos() throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/reportes/derechos-activos");
        Request req = authJson(new Request.Builder().url(url).get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            return mapper.readValue(res.body().byteStream(), new TypeReference<List<DerechoActivoItem>>() {
            });
        }
    }

    public byte[] exportCsv(String tipo, LocalDate desde, LocalDate hasta) throws IOException {
        // tipo: ingresos|asistencias|rechazos|derechos-activos (coinciden con endpoints
        // CSV)
        HttpUrl.Builder b = HttpUrl.parse(baseUrl + "/api/reportes/" + tipo + ".csv").newBuilder();
        if (!"derechos-activos".equals(tipo)) {
            if (desde != null)
                b.addQueryParameter("desde",
                        desde.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).toString());
            if (hasta != null)
                b.addQueryParameter("hasta", hasta.plusDays(1).atStartOfDay().minusNanos(1)
                        .atOffset(OffsetDateTime.now().getOffset()).toString());
        }
        Request req = new Request.Builder()
                .url(b.build())
                .addHeader("Authorization", "Bearer " + bearer)
                .addHeader("Accept", "text/csv")
                .get().build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            return res.body() != null ? res.body().bytes() : new byte[0];
        }
    }

    // ---- DTOs cliente ----
    public static final class IngresosDiariosItem {
        public LocalDate dia;
        public String tipoPago;
        public String metodoPago;
        public Long cantidad;
        public BigDecimal total;
    }

    public static final class AsistenciasDiariasItem {
        public LocalDate dia;
        public Long totalAccesos;
        public Long usuariosUnicos;
    }

    public static final class RechazosDiariosItem {
        public LocalDate dia;
        public String motivoRechazo;
        public Long cantidad;
    }

    public static final class DerechoActivoItem {
        public String usuarioDocumento;
        public String usuarioNombre;
        public String tipoDerecho;
        public OffsetDateTime vigenteDesde;
        public OffsetDateTime vigenteHasta;
        public Integer diasRestantes;
    }
}
