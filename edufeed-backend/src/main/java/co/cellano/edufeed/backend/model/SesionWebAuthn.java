package co.cellano.edufeed.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa una sesión de autenticación WebAuthn activa.
 * Se usa para tracking del flujo de registro/autenticación vía QR.
 */
@Entity
@Table(name = "sesiones_webauthn")
public class SesionWebAuthn {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "challenge", nullable = false, length = 255)
    private String challenge;

    @Column(name = "usuario_documento", length = 20)
    private String usuarioDocumento;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // REGISTRO | AUTENTICACION

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "PENDIENTE"; // PENDIENTE | COMPLETADA | EXPIRADA | FALLIDA

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "completado_en")
    private OffsetDateTime completadoEn;

    @Column(name = "resultado", columnDefinition = "jsonb")
    private String resultado; // JSON con resultado de la autenticación

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }
}
