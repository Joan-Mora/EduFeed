package co.cellano.edufeed.perf;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessCheckSmoke {
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\s*:\s*\"([^\"]+)\"");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"accessToken\"\s*:\s*\"([^\"]+)\"");

    public static void main(String[] args) throws Exception {
        String baseUrl = getArg(args, "-baseUrl", System.getProperty("baseUrl", "http://localhost:8080"));
        int users = Integer.parseInt(getArg(args, "-users", String.valueOf(Integer.getInteger("users", 20))));
        String username = System.getProperty("perf.username", System.getenv().getOrDefault("SEED_OPERATOR_USERNAME", "admin"));
        String password = System.getProperty("perf.password", System.getenv().getOrDefault("SEED_OPERATOR_PASSWORD", "Admin123$"));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        String token = login(client, baseUrl, username, password);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No se pudo obtener token JWT en login");
        }

        System.out.printf("Smoke AccessCheck: baseUrl=%s users=%d\n", baseUrl, users);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(users, 32));
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            tasks.add(() -> {
                String usuarioId = createUser(client, baseUrl, token);
                if (usuarioId == null) throw new IllegalStateException("No se pudo crear usuario");
                long t0 = System.nanoTime();
                int status = checkAccess(client, baseUrl, token, usuarioId);
                long t1 = System.nanoTime();
                latenciesMs.add((t1 - t0) / 1_000_000);
                if (status != 200) throw new IllegalStateException("/api/accesos/verificar status=" + status);
                return null;
            });
        }

        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) f.get();
        pool.shutdown();

        // métricas simples
        List<Long> copy = new ArrayList<>(latenciesMs);
        Collections.sort(copy);
        long p95 = percentile(copy, 95);
        long p50 = percentile(copy, 50);
        long max = copy.get(copy.size() - 1);
        double avg = copy.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.printf("Resultados AccessCheck -> n=%d, p50=%dms, p95=%dms, max=%dms, avg=%.1fms\n",
                copy.size(), p50, p95, max, avg);
    }

    private static long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    private static String login(HttpClient client, String baseUrl, String user, String pass) throws IOException, InterruptedException {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", escape(user), escape(pass));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) return null;
        Matcher m = TOKEN_PATTERN.matcher(resp.body());
        return m.find() ? m.group(1) : null;
    }

    private static String createUser(HttpClient client, String baseUrl, String token) throws IOException, InterruptedException {
        String documento = String.valueOf(Math.abs(new Random().nextInt()));
        String json = "{" +
                "\"documento\":\"" + documento + "\"," +
                "\"nombreCompleto\":\"Usuario " + documento + "\"," +
                "\"tipoUsuario\":\"ESTUDIANTE\"," +
                "\"email\":\"user" + documento + "@example.com\"," +
                "\"telefono\":\"300" + documento + "\"," +
                "\"activo\":true}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/usuarios"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200 && resp.statusCode() != 201) return null;
        Matcher m = ID_PATTERN.matcher(resp.body());
        return m.find() ? m.group(1) : null;
    }

    private static int checkAccess(HttpClient client, String baseUrl, String token, String usuarioId) throws IOException, InterruptedException {
        String json = String.format("{\"usuarioId\":\"%s\",\"modalidad\":\"HUELLA\"}", escape(usuarioId));
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/accesos/verificar"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return resp.statusCode();
    }

    private static String getArg(String[] args, String name, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) return args[i + 1];
        }
        return def;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
