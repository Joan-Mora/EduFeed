package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.domain.Operador;
import co.cellano.edufeed.backend.repository.OperadorRepository;
import co.cellano.edufeed.backend.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final OperadorRepository operadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(OperadorRepository operadorRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.operadorRepository = operadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public TokenPair login(String username, String rawPassword) {
        Operador op = operadorRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña inválidos"));
        if (!op.isActivo() || !passwordEncoder.matches(rawPassword, op.getPasswordHash())) {
            throw new IllegalArgumentException("Usuario o contraseña inválidos");
        }
        List<String> roles = parseRoles(op.getRoles());
        String access = tokenProvider.generateAccessToken(op.getId(), op.getUsername(), roles);
        String refresh = tokenProvider.generateRefreshToken(op.getId(), op.getUsername(), roles);
        return new TokenPair(access, refresh);
    }

    public TokenPair refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido");
        }
        var userId = tokenProvider.getUserId(refreshToken);
        var username = tokenProvider.getUsername(refreshToken);
        var roles = tokenProvider.getRoles(refreshToken);
        String access = tokenProvider.generateAccessToken(userId, username, roles);
        // Opcional: rotar refresh token
        String newRefresh = tokenProvider.generateRefreshToken(userId, username, roles);
        return new TokenPair(access, newRefresh);
    }

    private List<String> parseRoles(String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) return List.of();
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
