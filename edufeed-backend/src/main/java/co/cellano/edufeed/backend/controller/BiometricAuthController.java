package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.BiometricAuthRequest;
import co.cellano.edufeed.backend.dto.BiometricAuthResponse;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import co.cellano.edufeed.backend.service.BiometricAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador para autenticación biométrica desde dispositivos móviles.
 * 
 * Flujo:
 * 1. Desktop genera QR con URL: /auth/biometric?userId=XXX&token=YYY
 * 2. Móvil escanea QR y abre esta página
 * 3. Página muestra opciones: Huella, FaceID, Voz
 * 4. Usuario autentica con método elegido
 * 5. Backend valida y notifica al desktop vía WebSocket
 */
@Controller
@RequestMapping("/api/auth/biometric")
public class BiometricAuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BiometricAuthService biometricAuthService;

    /**
     * Página de autenticación biométrica (GET)
     * Renderiza HTML con opciones de autenticación
     */
    @GetMapping("")
    public String biometricAuthPage(
            @RequestParam("userId") String userId,
            @RequestParam("token") String token,
            Model model) {

        // Validar token (en producción, validar JWT)
        if (token == null || token.isEmpty()) {
            model.addAttribute("error", "Token inválido");
            return "error";
        }

        // Buscar usuario por UUID o documento
        Optional<Usuario> usuario;
        try {
            // Intentar como UUID primero
            java.util.UUID uuid = java.util.UUID.fromString(userId);
            usuario = usuarioRepository.findById(uuid);
        } catch (IllegalArgumentException e) {
            // Si no es UUID, buscar por documento
            usuario = usuarioRepository.findByDocumento(userId);
        }

        if (usuario.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado: " + userId);
            return "error";
        }

        Usuario user = usuario.get();

        // Verificar qué métodos biométricos tiene registrados (simulación)
        boolean tieneHuella = biometricAuthService.hasFingerprint(user);
        boolean tieneFaceId = biometricAuthService.hasFaceId(user);
        boolean tieneVoz = biometricAuthService.hasVoice(user);

        // Preparar datos para la vista
        model.addAttribute("userName", user.getNombreCompleto());
        model.addAttribute("userId", userId);
        model.addAttribute("token", token);
        model.addAttribute("tieneHuella", tieneHuella);
        model.addAttribute("tieneFaceId", tieneFaceId);
        model.addAttribute("tieneVoz", tieneVoz);

        // Si no tiene ningún método registrado
        if (!tieneHuella && !tieneFaceId && !tieneVoz) {
            model.addAttribute("error", "No tienes métodos biométricos registrados. Contacta al administrador.");
            return "biometric-auth";
        }

        return "biometric-auth";
    }

    /**
     * Endpoint para verificar autenticación biométrica (POST)
     */
    @PostMapping("/verify")
    @ResponseBody
    public ResponseEntity<BiometricAuthResponse> verifyBiometric(
            @RequestBody BiometricAuthRequest request) {

        try {
            // Buscar usuario por UUID o documento
            Optional<Usuario> usuario;
            try {
                // Intentar como UUID primero
                java.util.UUID uuid = java.util.UUID.fromString(request.getUserId());
                usuario = usuarioRepository.findById(uuid);
            } catch (IllegalArgumentException e) {
                // Si no es UUID, buscar por documento
                usuario = usuarioRepository.findByDocumento(request.getUserId());
            }

            if (usuario.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new BiometricAuthResponse(false, "Usuario no encontrado: " + request.getUserId(), null));
            }

            Usuario user = usuario.get();

            // Verificar según el método elegido
            boolean verified = false;
            String message = "";

            switch (request.getMethod()) {
                case "fingerprint":
                    verified = biometricAuthService.verifyFingerprint(user, request.getData());
                    message = verified ? "Huella verificada correctamente" : "Huella no coincide";
                    break;

                case "faceid":
                    verified = biometricAuthService.verifyFaceId(user, request.getData());
                    message = verified ? "FaceID verificado correctamente" : "Rostro no reconocido";
                    break;

                case "voice":
                    verified = biometricAuthService.verifyVoice(user, request.getData());
                    message = verified ? "Voz verificada correctamente" : "Voz no reconocida";
                    break;

                default:
                    return ResponseEntity.badRequest()
                            .body(new BiometricAuthResponse(false, "Método de autenticación no válido", null));
            }

            if (verified) {
                // Notificar al desktop vía WebSocket
                biometricAuthService.notifyDesktop(request.getUserId(), user);

                // Obtener datos del usuario para retornar
                Map<String, Object> userData = biometricAuthService.getUserPaymentData(user);

                return ResponseEntity.ok(new BiometricAuthResponse(true, message, userData));
            } else {
                // Si la primera verificación falla, solicitar segunda
                boolean requiresSecondFactor = !request.isSecondAttempt();

                BiometricAuthResponse response = new BiometricAuthResponse(false, message, null);
                response.setRequiresSecondFactor(requiresSecondFactor);

                if (requiresSecondFactor) {
                    response.setMessage("Autenticación insuficiente. Por favor, verifica con un segundo método.");
                }

                return ResponseEntity.status(401).body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new BiometricAuthResponse(false, "Error interno del servidor: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint para verificar el estado de autenticación (polling desde desktop)
     */
    @GetMapping("/status/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkAuthStatus(@PathVariable String userId) {
        Map<String, Object> status = biometricAuthService.getAuthStatus(userId);
        return ResponseEntity.ok(status);
    }
}
