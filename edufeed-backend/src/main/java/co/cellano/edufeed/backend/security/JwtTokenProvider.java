package co.cellano.edufeed.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessValiditySeconds;
    private final long refreshValiditySeconds;

    public JwtTokenProvider(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.accessTokenValiditySeconds}") long accessValiditySeconds,
            @Value("${security.jwt.refreshTokenValiditySeconds}") long refreshValiditySeconds) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessValiditySeconds = accessValiditySeconds;
        this.refreshValiditySeconds = refreshValiditySeconds;
    }

    public String generateAccessToken(UUID userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, accessValiditySeconds, "access");
    }

    public String generateRefreshToken(UUID userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, refreshValiditySeconds, "refresh");
    }

    private String buildToken(UUID userId, String username, List<String> roles, long validitySeconds, String type) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(validitySeconds);
        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(issuer)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("userId", userId.toString())
                .claim("roles", roles)
                .claim("type", type)
                // Usamos la API nueva de jjwt 0.12.x porque la vieja está deprecada
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public String getUsername(String token) {
        return getAllClaims(token).getSubject();
    }

    public UUID getUserId(String token) {
        String id = getAllClaims(token).get("userId", String.class);
        return id != null ? UUID.fromString(id) : null;
    }

    public List<String> getRoles(String token) {
        Object roles = getAllClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public boolean isRefreshToken(String token) {
        String type = getAllClaims(token).get("type", String.class);
        return "refresh".equals(type);
    }
}
