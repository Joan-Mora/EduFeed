package co.cellano.edufeed.backend.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para estado de sesión WebAuthn.
 */
public class EstadoSesionWebAuthnResponse {
    private UUID sesionId;
    private String estado; // PENDIENTE | COMPLETADA | EXPIRADA | FALLIDA
    private String tipo; // REGISTRO | AUTENTICACION
    private OffsetDateTime creadoEn;
    private OffsetDateTime expiraEn;
    private OffsetDateTime completadoEn;
    private String mensaje;
    private Boolean exito;
    // Campos extra para que la PWA pueda construir correctamente publicKey
    private String challenge; // Base64URL
    private String usuarioDocumento;
    private java.util.List<String> allowCredentials; // opcional, para AUTENTICACION

    public EstadoSesionWebAuthnResponse() {
    }

    public EstadoSesionWebAuthnResponse(UUID sesionId, String estado, String tipo) {
        this.sesionId = sesionId;
        this.estado = estado;
        this.tipo = tipo;
    }

    // Getters y setters

    public UUID getSesionId() {
        return sesionId;
    }

    public void setSesionId(UUID sesionId) {
        this.sesionId = sesionId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(OffsetDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public OffsetDateTime getCompletadoEn() {
        return completadoEn;
    }

    public void setCompletadoEn(OffsetDateTime completadoEn) {
        this.completadoEn = completadoEn;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Boolean getExito() {
        return exito;
    }

    public void setExito(Boolean exito) {
        this.exito = exito;
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

    public java.util.List<String> getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(java.util.List<String> allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
