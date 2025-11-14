package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import co.cellano.edufeed.backend.service.BiometricRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador para el registro de datos biométricos desde dispositivos
 * móviles/externos.
 * 
 * Flujo de registro:
 * 1. Desktop genera QR con URL:
 * /biometric/register?userId=XXX&token=YYY&type=HUELLA|ROSTRO|VOZ
 * 2. Dispositivo móvil/externo escanea QR y abre la página de registro
 * 3. Página solicita permisos (cámara/micrófono/huella) según el tipo
 * 4. Usuario completa el registro (captura datos biométricos)
 * 5. Backend guarda la plantilla biométrica
 * 6. Desktop consulta el estado periódicamente para detectar cuando se complete
 */
@Controller
@RequestMapping("/api/biometric/register")
public class BiometricRegistrationController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PlantillaBiometricaRepository plantillaRepository;

    @Autowired
    private BiometricRegistrationService registrationService;

    /**
     * Página principal de registro biométrico (GET)
     * URL: /api/biometric/register?userId=XXX&token=YYY&sessionId=ZZZ
     */
    @GetMapping("")
    public String registrationMainPage(
            @RequestParam("userId") String userId,
            @RequestParam("token") String token,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "type", required = false) String type,
            Model model) {

        // Validar y cargar usuario
        Optional<Usuario> usuario = findUsuario(userId);
        if (usuario.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado: " + userId);
            return "biometric-error";
        }

        Usuario user = usuario.get();

        // Si no hay sessionId, crear uno nuevo
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = registrationService.startSession(user);
        }

        // Preparar datos para la vista
        model.addAttribute("userName", user.getNombreCompleto());
        model.addAttribute("userId", user.getId().toString());
        model.addAttribute("token", token);
        model.addAttribute("sessionId", sessionId);
        // Si llega un tipo específico, pasarlo a la vista como onlyType:
        // huella|rostro|voz
        if (type != null && !type.isBlank()) {
            String t = type.trim().toLowerCase();
            if (t.equals("huella") || t.equals("rostro") || t.equals("voz")) {
                model.addAttribute("onlyType", t);
            }
        }

        return "biometric-register";
    }

    /**
     * Página de registro de huella digital (GET)
     * URL: /api/biometric/register/fingerprint?userId=XXX&token=YYY
     */
    @GetMapping("/fingerprint")
    public String fingerprintRegistrationPage(
            @RequestParam("userId") String userId,
            @RequestParam("token") String token,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            Model model) {

        // Validar y cargar usuario
        Optional<Usuario> usuario = findUsuario(userId);
        if (usuario.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado: " + userId);
            return "biometric-error";
        }

        Usuario user = usuario.get();

        // Preparar datos para la vista
        // Asegurar sessionId válido
        if (sessionId == null || sessionId.isBlank() || registrationService.getUserFromSession(sessionId) == null) {
            sessionId = registrationService.startSession(user);
        }

        model.addAttribute("userName", user.getNombreCompleto());
        model.addAttribute("userId", user.getId().toString());
        try {
            model.addAttribute("userDocumento", user.getDocumento());
        } catch (Exception ignore) {
            model.addAttribute("userDocumento", userId);
        }
        model.addAttribute("token", token);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("tipoRegistro", "huella");

        return "biometric-register-fingerprint";
    }

    /**
     * Página de registro de reconocimiento facial (GET)
     * URL: /api/biometric/register/face?userId=XXX&token=YYY
     */
    @GetMapping("/face")
    public String faceRegistrationPage(
            @RequestParam("userId") String userId,
            @RequestParam("token") String token,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            Model model) {

        Optional<Usuario> usuario = findUsuario(userId);
        if (usuario.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado: " + userId);
            return "biometric-error";
        }

        Usuario user = usuario.get();

        if (sessionId == null || sessionId.isBlank() || registrationService.getUserFromSession(sessionId) == null) {
            sessionId = registrationService.startSession(user);
        }
        model.addAttribute("userName", user.getNombreCompleto());
        model.addAttribute("userId", user.getId().toString());
        model.addAttribute("token", token);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("tipoRegistro", "rostro");

        return "biometric-register-face";
    }

    /**
     * Página de registro de voz (GET)
     * URL: /api/biometric/register/voice?userId=XXX&token=YYY
     */
    @GetMapping("/voice")
    public String voiceRegistrationPage(
            @RequestParam("userId") String userId,
            @RequestParam("token") String token,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            Model model) {

        Optional<Usuario> usuario = findUsuario(userId);
        if (usuario.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado: " + userId);
            return "biometric-error";
        }

        Usuario user = usuario.get();

        // Frase predefinida para registro de voz
        String fraseRegistro = "Mi voz es mi contraseña para acceder a EduFeed";

        if (sessionId == null || sessionId.isBlank() || registrationService.getUserFromSession(sessionId) == null) {
            sessionId = registrationService.startSession(user);
        }
        model.addAttribute("userName", user.getNombreCompleto());
        model.addAttribute("userId", user.getId().toString());
        model.addAttribute("token", token);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("tipoRegistro", "voz");
        model.addAttribute("fraseRegistro", fraseRegistro);

        return "biometric-register-voice";
    }

    /**
     * Endpoint para guardar datos de huella digital (POST)
     */
    @PostMapping("/fingerprint")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveFingerprintData(
            @RequestBody Map<String, Object> request) {

        try {
            String sessionId = (String) request.get("sessionId");
            String fingerprintData = (String) request.get("fingerprintData");

            // Validar datos
            if (sessionId == null || fingerprintData == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Datos incompletos"));
            }

            // Obtener usuario de la sesión
            Usuario usuario = registrationService.getUserFromSession(sessionId);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Sesión no encontrada o expirada"));
            }

            // Guardar plantilla de huella
            boolean saved = registrationService.saveFingerprint(usuario, fingerprintData, sessionId);

            if (saved) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Huella digital registrada correctamente",
                        "sessionId", sessionId));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Error al guardar la huella"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para guardar datos de reconocimiento facial (POST)
     */
    @PostMapping("/face")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveFaceData(
            @RequestBody Map<String, Object> request) {

        try {
            String sessionId = (String) request.get("sessionId");
            String faceData = (String) request.get("faceData");

            if (sessionId == null || faceData == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Datos incompletos"));
            }

            Usuario usuario = registrationService.getUserFromSession(sessionId);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Sesión no encontrada o expirada"));
            }

            boolean saved = registrationService.saveFaceData(usuario, faceData, sessionId);

            if (saved) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Reconocimiento facial registrado correctamente",
                        "sessionId", sessionId));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Error al guardar el reconocimiento facial"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para guardar datos de voz (POST)
     */
    @PostMapping("/voice")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveVoiceData(
            @RequestBody Map<String, Object> request) {

        try {
            String sessionId = (String) request.get("sessionId");
            String voiceData = (String) request.get("voiceData");

            if (sessionId == null || voiceData == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Datos incompletos"));
            }

            Usuario usuario = registrationService.getUserFromSession(sessionId);
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Sesión no encontrada o expirada"));
            }

            boolean saved = registrationService.saveVoiceData(usuario, voiceData, sessionId);

            if (saved) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Voz registrada correctamente",
                        "sessionId", sessionId));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Error al guardar la voz"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para consultar el estado del registro (GET)
     * El desktop lo llama periódicamente para saber cuándo se completó el registro
     */
    @GetMapping("/status/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRegistrationStatus(
            @PathVariable("userId") String userId,
            @RequestParam(name = "sessionId", required = false) String sessionId) {

        try {
            Optional<Usuario> usuario = findUsuario(userId);
            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            Map<String, Object> status = registrationService.getRegistrationStatus(usuario.get(), sessionId);
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar estado: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para iniciar una sesión de registro completo (POST)
     * Devuelve un sessionId que se usará para trackear el progreso
     */
    @PostMapping("/session/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startRegistrationSession(
            @RequestBody Map<String, String> request) {

        try {
            String userId = request != null ? request.get("userId") : null;

            // Validación temprana de entrada
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Parámetro 'userId' es requerido"));
            }

            Optional<Usuario> usuario = findUsuario(userId);
            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            String sessionId = registrationService.startSession(usuario.get());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "userId", usuario.get().getId().toString()));

        } catch (Exception e) {
            // Incluir clase de excepción para facilitar el diagnóstico
            String msg = e.getMessage();
            String error = "Error al iniciar sesión" + (msg != null && !msg.isBlank() ? ": " + msg : "");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", error, "exception", e.getClass().getSimpleName()));
        }
    }

    /**
     * Método auxiliar para buscar usuario por ID o documento
     */
    private Optional<Usuario> findUsuario(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            // Intentar como UUID
            UUID uuid = UUID.fromString(userId);
            return usuarioRepository.findById(uuid);
        } catch (IllegalArgumentException ex) {
            // Si no es UUID, buscar por documento
            try {
                return usuarioRepository.findByDocumento(userId);
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }
}
