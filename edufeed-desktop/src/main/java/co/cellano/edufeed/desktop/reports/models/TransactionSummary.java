package co.cellano.edufeed.desktop.reports.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Modelo resumen de una transacción para reportes financieros.
 * Contiene los datos esenciales de cada transacción de pago.
 */
public class TransactionSummary {
    private String referenciaExterna;
    private String usuarioNombre;
    private String usuarioDocumento;
    private String tipoPago;
    private String metodoPago;
    private BigDecimal monto;
    private String estadoPago;
    private OffsetDateTime fecha;
    private String motivo;
    private String motivoDevolucion;

    public TransactionSummary() {
    }

    public TransactionSummary(String referenciaExterna, String usuarioNombre, String usuarioDocumento,
            String tipoPago, String metodoPago, BigDecimal monto,
            String estadoPago, OffsetDateTime fecha) {
        this.referenciaExterna = referenciaExterna;
        this.usuarioNombre = usuarioNombre;
        this.usuarioDocumento = usuarioDocumento;
        this.tipoPago = tipoPago;
        this.metodoPago = metodoPago;
        this.monto = monto;
        this.estadoPago = estadoPago;
        this.fecha = fecha;
    }

    // Getters y Setters
    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getUsuarioDocumento() {
        return usuarioDocumento;
    }

    public void setUsuarioDocumento(String usuarioDocumento) {
        this.usuarioDocumento = usuarioDocumento;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public void setFecha(OffsetDateTime fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivoDevolucion() {
        return motivoDevolucion;
    }

    public void setMotivoDevolucion(String motivoDevolucion) {
        this.motivoDevolucion = motivoDevolucion;
    }
}
