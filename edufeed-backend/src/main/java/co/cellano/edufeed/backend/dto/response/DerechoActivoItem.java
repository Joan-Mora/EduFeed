package co.cellano.edufeed.backend.dto.response;

import java.time.OffsetDateTime;

/**
 * DTO para reporte de derechos de uso activos.
 * Lista derechos vigentes con información del usuario.
 * 
 * @since FASE 3.3
 */
public class DerechoActivoItem {
    private String usuarioDocumento;
    private String usuarioNombre;
    private String tipoDerecho;
    private OffsetDateTime vigenteDesde;
    private OffsetDateTime vigenteHasta;
    private Integer diasRestantes; // Solo para paquetes

    public String getUsuarioDocumento() {
        return usuarioDocumento;
    }

    public void setUsuarioDocumento(String usuarioDocumento) {
        this.usuarioDocumento = usuarioDocumento;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getTipoDerecho() {
        return tipoDerecho;
    }

    public void setTipoDerecho(String tipoDerecho) {
        this.tipoDerecho = tipoDerecho;
    }

    public OffsetDateTime getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(OffsetDateTime vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    public OffsetDateTime getVigenteHasta() {
        return vigenteHasta;
    }

    public void setVigenteHasta(OffsetDateTime vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    public Integer getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(Integer diasRestantes) {
        this.diasRestantes = diasRestantes;
    }
}
