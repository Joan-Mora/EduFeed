package co.cellano.edufeed.backend.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para iniciar registro/autenticación WebAuthn.
 */
public class IniciarWebAuthnResponse {
    private UUID sesionId;
    private String challenge;
    private String usuarioDocumento;
    private List<String> allowCredentials; // IDs de credenciales existentes (solo para autenticación)
    private String qrUrl; // URL para generar QR

    public IniciarWebAuthnResponse() {}

    public IniciarWebAuthnResponse(UUID sesionId, String challenge, String usuarioDocumento) {
        this.sesionId = sesionId;
        this.challenge = challenge;
        this.usuarioDocumento = usuarioDocumento;
        this.qrUrl = "/api/webauthn/qr/" + sesionId;
    }

    // Getters y setters

    public UUID getSesionId() {
        return sesionId;
    }

    public void setSesionId(UUID sesionId) {
        this.sesionId = sesionId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getUsuarioDocumento() {
        return usuarioDocumento;
    }

    public void setUsuarioDocumento(String usuarioDocumento) {
        this.usuarioDocumento = usuarioDocumento;
    }

    public List<String> getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(List<String> allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }
}
