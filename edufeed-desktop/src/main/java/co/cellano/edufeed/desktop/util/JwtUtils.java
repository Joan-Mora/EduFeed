package co.cellano.edufeed.desktop.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Utilidad mínima para decodificar roles de un JWT (sin validar firma). */
public final class JwtUtils {
    private JwtUtils() {
    }

    /**
     * Intenta extraer un nombre de usuario legible del JWT sin validar firma.
     * Orden de preferencia: preferred_username, username, name, sub.
     */
    public static String extractUsername(String jwt) {
        try {
            if (jwt == null || jwt.isBlank())
                return "";
            String[] parts = jwt.split("\\.");
            if (parts.length < 2)
                return "";
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = Json.minParse(payloadJson);
            Object u = null;
            if (payload != null) {
                u = firstNonNull(payload.get("preferred_username"), payload.get("username"),
                        payload.get("name"), payload.get("sub"));
            }
            return u != null ? String.valueOf(u) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static Object firstNonNull(Object... arr) {
        if (arr == null)
            return null;
        for (Object o : arr)
            if (o != null)
                return o;
        return null;
    }

    /**
     * Extrae el claim "roles" del payload del JWT como un conjunto de strings.
     * No valida la firma (solo para UI); en backend ya se valida.
     */
    public static Set<String> extractRoles(String jwt) {
        try {
            if (jwt == null || jwt.isBlank())
                return Collections.emptySet();
            String[] parts = jwt.split("\\.");
            if (parts.length < 2)
                return Collections.emptySet();
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = Json.minParse(payloadJson);
            Object r = payload.get("roles");
            if (r instanceof Iterable<?> it) {
                Set<String> s = new HashSet<>();
                for (Object o : it)
                    if (o != null)
                        s.add(o.toString());
                return s;
            }
            return Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * Parser JSON mínimo para estructuras simples (mapas/arrays/strings/números).
     */
    static class Json {
        // Para evitar introducir dependencias, un parseo muy básico usando org.json
        // no está disponible; implementamos con com.fasterxml si existiera, pero aquí
        // haremos un fallback muy simple: solo detecta arrays de strings.
        public static Map<String, Object> minParse(String json) {
            // Intentar usar Jackson si está en el classpath del módulo Desktop
            try {
                Class<?> mapperClz = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
                Object mapper = mapperClz.getDeclaredConstructor().newInstance();
                Object obj = mapperClz
                        .getMethod("readValue", String.class, Class.class)
                        .invoke(mapper, json, Map.class);
                if (obj instanceof Map<?, ?> m) {
                    // Copiar a Map<String,Object>
                    java.util.Map<String, Object> out = new java.util.HashMap<>();
                    for (var e : m.entrySet()) {
                        out.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    return out;
                }
                return java.util.Collections.emptyMap();
            } catch (Throwable ignore) {
                // como fallback, devolver vacío
                return java.util.Collections.emptyMap();
            }
        }
    }
}
