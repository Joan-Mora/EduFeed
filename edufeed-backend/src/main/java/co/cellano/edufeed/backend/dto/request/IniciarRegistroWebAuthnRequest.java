package co.cellano.edufeed.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para iniciar registro WebAuthn.
 */
public class IniciarRegistroWebAuthnRequest {
    @NotBlank(message = "El documento del usuario es obligatorio")
    private String usuarioDocumento;

    private String plataforma; // ANDROID | IOS | WEB
    private String modelo;

    public String getUsuarioDocumento() {
        return usuarioDocumento;
    }

    public void setUsuarioDocumento(String usuarioDocumento) {
        this.usuarioDocumento = usuarioDocumento;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(String plataforma) {
        this.plataforma = plataforma;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }
}
