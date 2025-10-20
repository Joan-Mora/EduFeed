package co.cellano.edufeed.backend.dto;

import co.cellano.edufeed.backend.model.enums.TipoPago;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public class DerechoUsoDto {
    private String id;

    @NotBlank
    private String usuarioId;

    @NotNull
    private TipoPago tipoDerecho; // DIARIO | MENSUAL | PAQUETE

    private String pagoOrigenId;

    @NotNull
    private OffsetDateTime vigenteDesde;

    private OffsetDateTime vigenteHasta;

    private Boolean activo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public TipoPago getTipoDerecho() {
        return tipoDerecho;
    }

    public void setTipoDerecho(TipoPago tipoDerecho) {
        this.tipoDerecho = tipoDerecho;
    }

    public String getPagoOrigenId() {
        return pagoOrigenId;
    }

    public void setPagoOrigenId(String pagoOrigenId) {
        this.pagoOrigenId = pagoOrigenId;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
