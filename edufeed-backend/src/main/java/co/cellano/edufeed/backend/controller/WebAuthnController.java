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
import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webauthn")
@Tag(name = "WebAuthn", description = "Autenticación biométrica vía teléfono usando WebAuthn/FIDO2")
public class WebAuthnController {

    private final WebAuthnService webAuthnService;

    public WebAuthnController(WebAuthnService webAuthnService) {
        this.webAuthnService = webAuthnService;
    }

    @PostMapping("/registro/iniciar")
    @Operation(summary = "Iniciar registro WebAuthn", description = "Inicia el proceso de registro de credencial biométrica. Genera un challenge y retorna la URL del QR para escanear con el teléfono.")
    public ResponseEntity<IniciarWebAuthnResponse> iniciarRegistro(
            @Valid @RequestBody IniciarRegistroWebAuthnRequest request) {
        IniciarWebAuthnResponse response = webAuthnService.iniciarRegistro(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/registro/{sesionId}/completar")
    @Operation(summary = "Completar registro WebAuthn", description = "Completa el registro de credencial biométrica con los datos capturados del teléfono.")
    public ResponseEntity<EstadoSesionWebAuthnResponse> completarRegistro(
            @Parameter(description = "ID de la sesión de registro") @PathVariable("sesionId") UUID sesionId,
            @Valid @RequestBody CompletarRegistroWebAuthnRequest request) {
        EstadoSesionWebAuthnResponse response = webAuthnService.completarRegistro(sesionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/autenticacion/iniciar")
    @Operation(summary = "Iniciar autenticación WebAuthn", description = "Inicia el proceso de autenticación biométrica. Genera un challenge y retorna las credenciales permitidas.")
    public ResponseEntity<IniciarWebAuthnResponse> iniciarAutenticacion(
            @Valid @RequestBody IniciarAutenticacionWebAuthnRequest request) {
        IniciarWebAuthnResponse response = webAuthnService.iniciarAutenticacion(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/autenticacion/{sesionId}/completar")
    @Operation(summary = "Completar autenticación WebAuthn", description = "Completa la autenticación biométrica verificando la firma digital.")
    public ResponseEntity<EstadoSesionWebAuthnResponse> completarAutenticacion(
            @Parameter(description = "ID de la sesión de autenticación") @PathVariable("sesionId") UUID sesionId,
            @Valid @RequestBody CompletarAutenticacionWebAuthnRequest request) {
        EstadoSesionWebAuthnResponse response = webAuthnService.completarAutenticacion(sesionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sesion/{sesionId}")
    @Operation(summary = "Obtener estado de sesión", description = "Consulta el estado actual de una sesión WebAuthn. Usado por el desktop para polling.")
    public ResponseEntity<EstadoSesionWebAuthnResponse> obtenerEstadoSesion(
            @Parameter(description = "ID de la sesión") @PathVariable("sesionId") UUID sesionId) {
        EstadoSesionWebAuthnResponse response = webAuthnService.obtenerEstadoSesion(sesionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr/{sesionId}")
    @Operation(summary = "Datos para generar QR", description = "Retorna los datos necesarios para generar el código QR que el usuario escaneará con su teléfono.")
    public ResponseEntity<Map<String, String>> obtenerDatosQR(
            @Parameter(description = "ID de la sesión") @PathVariable("sesionId") UUID sesionId,
            HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedPort = request.getHeader("X-Forwarded-Port");

        String base;
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            String proto = (forwardedProto != null && !forwardedProto.isBlank()) ? forwardedProto : request.getScheme();
            if (forwardedHost.contains(":")) {
                base = proto + "://" + forwardedHost;
            } else if (forwardedPort != null && !forwardedPort.isBlank()
                    && !("80".equals(forwardedPort) || "443".equals(forwardedPort))) {
                base = String.format("%s://%s:%s", proto, forwardedHost, forwardedPort);
            } else {
                base = String.format("%s://%s", proto, forwardedHost);
            }
        } else {
            String scheme = request.getScheme();
            String host = preferNonLoopbackHost(request);
            int port = request.getServerPort();
            base = (port == 80 || port == 443) ? String.format("%s://%s", scheme, host)
                    : String.format("%s://%s:%d", scheme, host, port);
        }
        String pwaUrl = String.format("%s/pwa-webauthn.html?sesionId=%s", base, sesionId);

        Map<String, String> qrData = Map.of(
                "sesionId", sesionId.toString(),
                "url", pwaUrl,
                "mensaje", "Escanea este código o abre el enlace desde tu teléfono");

        return ResponseEntity.ok(qrData);
    }

    private String preferNonLoopbackHost(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName == null)
            return request.getLocalAddr();
        String lower = serverName.toLowerCase();
        if (!"localhost".equals(lower) && !"127.0.0.1".equals(lower)) {
            return serverName;
        }
        // Intentar descubrir una IPv4 local utilizable (LAN)
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual())
                    continue;
                var addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    var addr = addrs.nextElement();
                    if (addr instanceof Inet4Address ipv4 && ipv4.isSiteLocalAddress()) {
                        return ipv4.getHostAddress();
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return request.getLocalAddr();
    }
}
