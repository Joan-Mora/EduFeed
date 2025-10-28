package co.cellano.edufeed.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para iniciar autenticación WebAuthn.
 */
public class IniciarAutenticacionWebAuthnRequest {
    @NotBlank(message = "El documento del usuario es obligatorio")
    private String usuarioDocumento;

    public String getUsuarioDocumento() {
        return usuarioDocumento;
    }

    public void setUsuarioDocumento(String usuarioDocumento) {
        this.usuarioDocumento = usuarioDocumento;
    }
}
