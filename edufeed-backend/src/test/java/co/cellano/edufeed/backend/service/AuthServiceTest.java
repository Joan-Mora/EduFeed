package co.cellano.edufeed.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.cellano.edufeed.backend.domain.Operador;
import co.cellano.edufeed.backend.repository.OperadorRepository;
import co.cellano.edufeed.backend.security.JwtTokenProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Login y Refresh")
class AuthServiceTest {

    @Mock
    OperadorRepository operadorRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider tokenProvider;

    @InjectMocks
    AuthService authService;

    private Operador buildOperador(boolean activo) {
        Operador op = new Operador();
        op.setId(UUID.randomUUID());
        op.setUsername("admin");
        op.setPasswordHash("hash");
        op.setRoles("ADMIN,USER");
        op.setActivo(activo);
        return op;
    }

    @Test
    @DisplayName("login: credenciales válidas retorna TokenPair")
    void login_ok() {
        Operador op = buildOperador(true);
        when(operadorRepository.findByUsername("admin")).thenReturn(Optional.of(op));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tokenProvider.generateAccessToken(op.getId(), op.getUsername(), List.of("ADMIN", "USER")))
                .thenReturn("access");
        when(tokenProvider.generateRefreshToken(op.getId(), op.getUsername(), List.of("ADMIN", "USER")))
                .thenReturn("refresh");

        AuthService.TokenPair pair = authService.login("admin", "secret");

        assertThat(pair).isNotNull();
        assertThat(pair.accessToken()).isEqualTo("access");
        assertThat(pair.refreshToken()).isEqualTo("refresh");
    }

    @Test
    @DisplayName("login: usuario inactivo o password incorrecto lanza IllegalArgumentException")
    void login_fail_invalido() {
        // usuario no existe
        when(operadorRepository.findByUsername("admin")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login("admin", "secret"))
                .isInstanceOf(IllegalArgumentException.class);

        // usuario inactivo
        Operador opInactivo = buildOperador(false);
        when(operadorRepository.findByUsername("admin")).thenReturn(Optional.of(opInactivo));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        assertThatThrownBy(() -> authService.login("admin", "secret"))
                .isInstanceOf(IllegalArgumentException.class);

        // password incorrecto
        Operador opActivo = buildOperador(true);
        when(operadorRepository.findByUsername("admin")).thenReturn(Optional.of(opActivo));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(false);
        assertThatThrownBy(() -> authService.login("admin", "secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refresh: token válido devuelve nuevos tokens")
    void refresh_ok() {
        UUID uid = UUID.randomUUID();
        when(tokenProvider.validateToken("r"))
                .thenReturn(true);
        when(tokenProvider.isRefreshToken("r")).thenReturn(true);
        when(tokenProvider.getUserId("r")).thenReturn(uid);
        when(tokenProvider.getUsername("r")).thenReturn("admin");
        when(tokenProvider.getRoles("r")).thenReturn(List.of("ADMIN"));
        when(tokenProvider.generateAccessToken(uid, "admin", List.of("ADMIN")))
                .thenReturn("new-access");
        when(tokenProvider.generateRefreshToken(uid, "admin", List.of("ADMIN")))
                .thenReturn("new-refresh");

        AuthService.TokenPair pair = authService.refresh("r");
        assertThat(pair.accessToken()).isEqualTo("new-access");
        assertThat(pair.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("refresh: token inválido lanza IllegalArgumentException")
    void refresh_fail() {
        when(tokenProvider.validateToken("bad")).thenReturn(false);
        assertThatThrownBy(() -> authService.refresh("bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
