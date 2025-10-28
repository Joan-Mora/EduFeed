package co.cellano.edufeed.backend.api;

import co.cellano.edufeed.backend.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        var pair = authService.login(request.username(), request.password());
        Map<String, Object> body = new HashMap<>();
        body.put("tokenType", "Bearer");
        body.put("accessToken", pair.accessToken());
        body.put("refreshToken", pair.refreshToken());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        var pair = authService.refresh(request.refreshToken());
        Map<String, Object> body = new HashMap<>();
        body.put("tokenType", "Bearer");
        body.put("accessToken", pair.accessToken());
        body.put("refreshToken", pair.refreshToken());
        return ResponseEntity.ok(body);
    }
}
