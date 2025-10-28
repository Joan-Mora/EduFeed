package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.request.CompletarAutenticacionWebAuthnRequest;
import co.cellano.edufeed.backend.dto.request.CompletarRegistroWebAuthnRequest;
import co.cellano.edufeed.backend.dto.request.IniciarAutenticacionWebAuthnRequest;
import co.cellano.edufeed.backend.dto.request.IniciarRegistroWebAuthnRequest;
import co.cellano.edufeed.backend.dto.response.EstadoSesionWebAuthnResponse;
import co.cellano.edufeed.backend.dto.response.IniciarWebAuthnResponse;
import co.cellano.edufeed.backend.service.WebAuthnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación WebAuthn (huella por teléfono).
 */
@RestController
@RequestMapping("/api/webauthn")
@Tag(name = "WebAuthn", description = "Autenticación biométrica vía teléfono usando WebAuthn/FIDO2")
public class WebAuthnController {

    private final WebAuthnService webAuthnService;

    public WebAuthnController(WebAuthnService webAuthnService) {
        this.webAuthnService = webAuthnService;
    }

    // ============ REGISTRO ============

    @PostMapping("/registro/iniciar")
    @Operation(
        summary = "Iniciar registro WebAuthn",
        description = "Inicia el proceso de registro de credencial biométrica. Genera un challenge y retorna la URL del QR para escanear con el teléfono."
    )
    public ResponseEntity<IniciarWebAuthnResponse> iniciarRegistro(
            @Valid @RequestBody IniciarRegistroWebAuthnRequest request
    ) {
        IniciarWebAuthnResponse response = webAuthnService.iniciarRegistro(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/registro/{sesionId}/completar")
    @Operation(
        summary = "Completar registro WebAuthn",
        description = "Completa el registro de credencial biométrica con los datos capturados del teléfono."
    )
    public ResponseEntity<EstadoSesionWebAuthnResponse> completarRegistro(
            @Parameter(description = "ID de la sesión de registro")
        @PathVariable("sesionId") UUID sesionId,
            @Valid @RequestBody CompletarRegistroWebAuthnRequest request
    ) {
        EstadoSesionWebAuthnResponse response = webAuthnService.completarRegistro(sesionId, request);
        return ResponseEntity.ok(response);
    }

    // ============ AUTENTICACIÓN ============

    @PostMapping("/autenticacion/iniciar")
    @Operation(
        summary = "Iniciar autenticación WebAuthn",
        description = "Inicia el proceso de autenticación biométrica. Genera un challenge y retorna las credenciales permitidas."
    )
    public ResponseEntity<IniciarWebAuthnResponse> iniciarAutenticacion(
            @Valid @RequestBody IniciarAutenticacionWebAuthnRequest request
    ) {
        IniciarWebAuthnResponse response = webAuthnService.iniciarAutenticacion(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/autenticacion/{sesionId}/completar")
    @Operation(
        summary = "Completar autenticación WebAuthn",
        description = "Completa la autenticación biométrica verificando la firma digital."
    )
    public ResponseEntity<EstadoSesionWebAuthnResponse> completarAutenticacion(
            @Parameter(description = "ID de la sesión de autenticación")
        @PathVariable("sesionId") UUID sesionId,
            @Valid @RequestBody CompletarAutenticacionWebAuthnRequest request
    ) {
        EstadoSesionWebAuthnResponse response = webAuthnService.completarAutenticacion(sesionId, request);
        return ResponseEntity.ok(response);
    }

    // ============ POLLING Y QR ============

    @GetMapping("/sesion/{sesionId}")
    @Operation(
        summary = "Obtener estado de sesión",
        description = "Consulta el estado actual de una sesión WebAuthn. Usado por el desktop para polling."
    )
    public ResponseEntity<EstadoSesionWebAuthnResponse> obtenerEstadoSesion(
            @Parameter(description = "ID de la sesión")
        @PathVariable("sesionId") UUID sesionId
    ) {
        EstadoSesionWebAuthnResponse response = webAuthnService.obtenerEstadoSesion(sesionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr/{sesionId}")
    @Operation(
        summary = "Datos para generar QR",
        description = "Retorna los datos necesarios para generar el código QR que el usuario escaneará con su teléfono."
    )
    public ResponseEntity<Map<String, String>> obtenerDatosQR(
            @Parameter(description = "ID de la sesión")
        @PathVariable("sesionId") UUID sesionId
    ) {
        // El desktop/frontend generará el QR con esta URL
        // El teléfono abrirá la PWA mínima con estos parámetros
        String pwaUrl = String.format("https://edufeed.co/pwa/webauthn?sesionId=%s", sesionId);
        
        Map<String, String> qrData = Map.of(
                "sesionId", sesionId.toString(),
                "url", pwaUrl,
                "mensaje", "Escanea este código con tu teléfono para autenticarte"
        );
        
        return ResponseEntity.ok(qrData);
    }
}
