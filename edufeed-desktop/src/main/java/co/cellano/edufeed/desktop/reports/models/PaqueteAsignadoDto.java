package co.cellano.edufeed.desktop.reports.models;

import co.cellano.edufeed.desktop.service.PaymentApiClient.EstadoPago;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO que representa un paquete asignado a un usuario.
 * Corresponde a un pago de tipo PAQUETE en el sistema.
 */
public class PaqueteAsignadoDto {
    private UUID pagoId;
    private UUID usuarioId;
    private String usuarioDocumento;
    private String usuarioNombre;
    private PaqueteServicio paquete;
    private BigDecimal monto;
    private EstadoPago estado;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
    private String referenciaExterna;
    private String metodoPago;
    private Integer diasPaquete;

    public PaqueteAsignadoDto() {
    }

    // Getters y Setters
    public UUID getPagoId() {
        return pagoId;
    }

    public void setPagoId(UUID pagoId) {
        this.pagoId = pagoId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

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

    public PaqueteServicio getPaquete() {
        return paquete;
    }

    public void setPaquete(PaqueteServicio paquete) {
        this.paquete = paquete;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Integer getDiasPaquete() {
        return diasPaquete;
    }

    public void setDiasPaquete(Integer diasPaquete) {
        this.diasPaquete = diasPaquete;
    }

    /**
     * Retorna el estado formateado para UI.
     */
    public String getEstadoFormateado() {
        if (estado == null)
            return "DESCONOCIDO";
        return switch (estado) {
            case APROBADO -> "Aprobado";
            case PENDIENTE -> "Pendiente";
            case RECHAZADO -> "Rechazado";
            case REVERTIDO -> "Devolución";
        };
    }

    /**
     * Retorna el nombre del paquete o "N/A" si no está disponible.
     */
    public String getNombrePaquete() {
        return paquete != null ? paquete.getNombre() : "N/A";
    }
}
