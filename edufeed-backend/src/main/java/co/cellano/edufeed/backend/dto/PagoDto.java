package co.cellano.edufeed.backend.dto;

import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO para transferencia de datos de pagos.
 * 
 * @since FASE 1, extendido en FASE 2.2
 */
public class PagoDto {
    private String id;

    @NotBlank(message = "Usuario ID es requerido")
    private String usuarioId;

    @NotNull(message = "Monto es requerido")
    @Positive(message = "Monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotNull(message = "Tipo de pago es requerido")
    private TipoPago tipoPago;

    private EstadoPago estadoPago;

    private OffsetDateTime creadoEn;

    private OffsetDateTime vigenteDesde;

    private OffsetDateTime vigenteHasta;

    private String metodoPago;

    private String referenciaExterna;

    private String cajero;

    private String metadatos;

    /**
     * Solo para pagos tipo PAQUETE: cantidad de días del paquete.
     */
    @Positive(message = "Días del paquete debe ser mayor a cero")
    private Integer diasPaquete;

    // Getters y Setters

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

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public TipoPago getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(TipoPago tipoPago) {
        this.tipoPago = tipoPago;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
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

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getCajero() {
        return cajero;
    }

    public void setCajero(String cajero) {
        this.cajero = cajero;
    }

    public String getMetadatos() {
        return metadatos;
    }

    public void setMetadatos(String metadatos) {
        this.metadatos = metadatos;
    }

    public Integer getDiasPaquete() {
        return diasPaquete;
    }

    public void setDiasPaquete(Integer diasPaquete) {
        this.diasPaquete = diasPaquete;
    }
}
