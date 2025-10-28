package co.cellano.edufeed.backend.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class JwtTokenProviderTest {

    @Test
    void generateAndValidateAccessToken() {
        // Arrange: construir provider con secret fijo
        String secret = "ZmFrZS1zZWNyZXQtZGV2LXNob3VsZC1iZS0zMi1vci0xNi1ieXRlcw=="; // base64
        JwtTokenProvider provider = new JwtTokenProvider(secret, "EduFeed", 3600, 1209600);

        UUID userId = UUID.randomUUID();
        String username = "tester";
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_CAJERO");

        // Act
        String token = provider.generateAccessToken(userId, username, roles);

        // Assert
        assertTrue(provider.validateToken(token));
        Claims claims = provider.getAllClaims(token);
        assertEquals(username, claims.getSubject());
        assertEquals(userId.toString(), claims.get("userId"));
        assertEquals(roles, claims.get("roles"));
        assertEquals("access", claims.get("type"));
    }
}
