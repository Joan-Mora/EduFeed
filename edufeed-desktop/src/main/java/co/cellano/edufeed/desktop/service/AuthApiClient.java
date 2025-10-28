package co.cellano.edufeed.desktop.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import okhttp3.*;

public class AuthApiClient {
    private final OkHttpClient http;
    private final HttpUrl baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

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
            if (!res.isSuccessful()) throw new IOException("HTTP "+res.code()+" - "+(res.body()!=null?res.body().string():""));
            String s = res.body()!=null?res.body().string():"{}";
            LoginResponse r = mapper.readValue(s, LoginResponse.class);
            return new TokenPair(r.tokenType, r.accessToken, r.refreshToken);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginResponse {
        public String tokenType;
        public String accessToken;
        public String refreshToken;
    }

    public record TokenPair(String tokenType, String accessToken, String refreshToken) {}
}
