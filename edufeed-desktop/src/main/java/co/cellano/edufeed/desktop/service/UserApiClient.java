package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import okhttp3.*;

/** Cliente para gestión de usuarios y biometría (ADMIN). */
public class UserApiClient {
    private final OkHttpClient http;
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final String bearer;

    public UserApiClient(String baseUrl, String bearer) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bearer = bearer;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .writeTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    private Request.Builder authJson(Request.Builder b) {
        return b.addHeader("Authorization", "Bearer " + bearer)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");
    }

    // ---- Listado con paginación/filtrado ----
    public PageResponse<UsuarioDto> listPaged(int page, int size, Filters f) throws IOException {
        // Estrategia: si hay documento -> devuelve página de 1; si hay nombre -> buscar
        // y paginar en cliente; si no filtros -> usar paginado backend
        if (f != null && f.documento() != null && !f.documento().isBlank()) {
            Optional<UsuarioDto> uno = buscarPorDocumento(f.documento());
            List<UsuarioDto> content = uno.map(List::of).orElse(List.of());
            return PageResponse.of(content, content.size(), 1, 0, content.size());
        }
        if (f != null && f.nombre() != null && !f.nombre().isBlank()) {
            List<UsuarioDto> all = buscarPorNombre(f.nombre());
            all = filtrarTipoYActivo(all, f);
            return paginarCliente(all, page, size);
        }
        if (f != null && ((f.tipo() != null && !f.tipo().isBlank()) || Boolean.TRUE.equals(f.soloActivos()))) {
            List<UsuarioDto> all = listAll(Boolean.TRUE.equals(f.soloActivos()));
            all = filtrarTipoYActivo(all, f);
            return paginarCliente(all, page, size);
        }
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios")
                .newBuilder().addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("size", String.valueOf(size)).build();
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "{}";
            System.out.println("[UserApiClient] GET " + url + " -> code=" + code + ", body=" + body);
            if (!res.isSuccessful())
                throw new IOException("HTTP " + code + " body=" + body);
            PageResponse<UsuarioDto> p = mapper.readValue(body, new TypeReference<PageResponse<UsuarioDto>>() {
            });
            // Fallback por desajuste de indexación (algunos backends usan 1-based)
            if ((p.content == null || p.content.isEmpty()) && p.totalElements > 0 && page == 0) {
                HttpUrl url2 = HttpUrl.parse(baseUrl + "/api/usuarios")
                        .newBuilder().addQueryParameter("page", "1")
                        .addQueryParameter("size", String.valueOf(size)).build();
                Request req2 = authJson(new Request.Builder().get().url(url2)).build();
                try (Response res2 = http.newCall(req2).execute()) {
                    if (res2.isSuccessful()) {
                        String body2 = res2.body() != null ? res2.body().string() : "{}";
                        System.out.println("[UserApiClient] Fallback page=1 body=" + body2);
                        return mapper.readValue(body2, new TypeReference<PageResponse<UsuarioDto>>() {
                        });
                    }
                }
            }
            // Fallback adicional: si sigue vacío pero totalElements>0, degradar a listAll y
            // paginar en cliente
            if ((p.content == null || p.content.isEmpty()) && p.totalElements > 0) {
                try {
                    List<UsuarioDto> all = listAll(false);
                    System.out.println("[UserApiClient] Fallback listAll size=" + (all != null ? all.size() : -1));
                    return paginarCliente(all != null ? all : List.of(), page, size);
                } catch (IOException ioe) {
                    System.out.println("[UserApiClient] Fallback listAll failed: " + ioe.getMessage());
                }
            }
            return p;
        }
    }

    private List<UsuarioDto> filtrarTipoYActivo(List<UsuarioDto> in, Filters f) {
        if (in == null)
            return List.of();
        return in.stream()
                .filter(u -> f.tipo() == null || f.tipo().isBlank()
                        || (u.tipoUsuario != null && u.tipoUsuario.equalsIgnoreCase(f.tipo())))
                .filter(u -> f.soloActivos() == null || !f.soloActivos() || Boolean.TRUE.equals(u.activo))
                .toList();
    }

    private PageResponse<UsuarioDto> paginarCliente(List<UsuarioDto> all, int page, int size) {
        int from = Math.max(0, Math.min(page * size, all.size()));
        int to = Math.max(from, Math.min(from + size, all.size()));
        List<UsuarioDto> content = all.subList(from, to);
        int totalPages = (int) Math.ceil(all.size() / (double) size);
        return PageResponse.of(content, all.size(), totalPages, page, size);
    }

    public List<UsuarioDto> listAll(boolean soloActivos) throws IOException {
        HttpUrl.Builder b = HttpUrl.parse(baseUrl + "/api/usuarios").newBuilder();
        if (soloActivos)
            b.addQueryParameter("soloActivos", "true");
        Request req = authJson(new Request.Builder().get().url(b.build())).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "[]";
            return mapper.readValue(body, new TypeReference<List<UsuarioDto>>() {
            });
        }
    }

    public Optional<UsuarioDto> buscarPorDocumento(String documento) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/buscar/documento/" + documento);
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() == 404)
                return Optional.empty();
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "{}";
            return Optional.of(mapper.readValue(body, UsuarioDto.class));
        }
    }

    public List<UsuarioDto> buscarPorNombre(String q) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/api/usuarios/buscar/nombre").newBuilder()
                .addQueryParameter("q", q).build();
        Request req = authJson(new Request.Builder().get().url(url)).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "[]";
            return mapper.readValue(body, new TypeReference<List<UsuarioDto>>() {
            });
        }
    }

    // ---- CRUD ----
    public UsuarioDto create(UsuarioDto u) throws IOException {
        String json = mapper.writeValueAsString(u);
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios")
                .post(RequestBody.create(json, MediaType.parse("application/json"))))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "{}";
            return mapper.readValue(body, UsuarioDto.class);
        }
    }

    public UsuarioDto update(String id, UsuarioDto u) throws IOException {
        String json = mapper.writeValueAsString(u);
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + id)
                .put(RequestBody.create(json, MediaType.parse("application/json"))))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "{}";
            return mapper.readValue(body, UsuarioDto.class);
        }
    }

    public void desactivar(String id) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + id)
                .delete()).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() != 204 && !res.isSuccessful())
                throw new IOException("HTTP " + res.code());
        }
    }

    // Reactiva un usuario previamente desactivado (soft delete reversal)
    public void reactivar(String id) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + id + "/reactivar")
                .post(RequestBody.create("{}", MediaType.parse("application/json")))).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
        }
    }

    public UsuarioDto get(String id) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + id)
                .get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "{}";
            return mapper.readValue(body, UsuarioDto.class);
        }
    }

    // ---- Biometría ----
    public List<PlantillaBiometricaDto> listarBiometrias(String userId) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + userId + "/biometria")
                .get()).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "[]";
            return mapper.readValue(body, new TypeReference<List<PlantillaBiometricaDto>>() {
            });
        }
    }

    public PlantillaBiometricaDto enrolar(String userId, Modalidad modalidad) throws IOException {
        String json = String.format("{\"modalidad\":\"%s\"}", modalidad.name());
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + userId + "/biometria/enrolar")
                .post(RequestBody.create(json, MediaType.parse("application/json"))))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            String body = res.body() != null ? res.body().string() : "{}";
            return mapper.readValue(body, PlantillaBiometricaDto.class);
        }
    }

    public void desactivarBiometria(String userId, String plantillaId) throws IOException {
        Request req = authJson(new Request.Builder()
                .url(baseUrl + "/api/usuarios/" + userId + "/biometria/" + plantillaId)
                .delete()).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() != 204 && !res.isSuccessful())
                throw new IOException("HTTP " + res.code());
        }
    }

    // ---- Tipos y DTOs ----
    public record Filters(String documento, String nombre, String tipo, Boolean soloActivos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageResponse<T> {
        public List<T> content;
        public long totalElements;
        public int totalPages;
        public int number;
        public int size;

        public static <T> PageResponse<T> of(List<T> content, long total, int totalPages, int number, int size) {
            PageResponse<T> p = new PageResponse<>();
            p.content = content;
            p.totalElements = total;
            p.totalPages = totalPages;
            p.number = number;
            p.size = size;
            return p;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsuarioDto {
        public String id;
        public String documento;
        public String nombreCompleto;
        public String tipoUsuario; // string del enum
        public String email;
        public String telefono;
        public Boolean activo;
    }

    public enum Modalidad {
        HUELLA, ROSTRO, VOZ, MANUAL
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlantillaBiometricaDto {
        public String id;
        public String modalidad;
        public String proveedor;
        public String creadoEn;
        public Boolean activo;
    }
}
