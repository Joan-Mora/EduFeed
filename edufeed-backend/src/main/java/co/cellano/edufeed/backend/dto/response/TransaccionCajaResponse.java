package co.cellano.edufeed.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para transacciones de caja/POS.
 */
public class TransaccionCajaResponse {
    private UUID id;
    private String proveedor;
    private String referenciaExterna;
    private BigDecimal monto;
    private String metodoPago;
    private String estado;
    private OffsetDateTime recibidoEn;
    private boolean conciliado;
    private UUID pagoId;
    private String usuarioDocumento;
    private String usuarioNombre;

    // Constructores
    public TransaccionCajaResponse() {}

    public TransaccionCajaResponse(UUID id, String proveedor, String referenciaExterna, 
                                   BigDecimal monto, String metodoPago, String estado,
                                   OffsetDateTime recibidoEn, boolean conciliado,
                                   UUID pagoId, String usuarioDocumento, String usuarioNombre) {
        this.id = id;
        this.proveedor = proveedor;
        this.referenciaExterna = referenciaExterna;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.estado = estado;
        this.recibidoEn = recibidoEn;
        this.conciliado = conciliado;
        this.pagoId = pagoId;
        this.usuarioDocumento = usuarioDocumento;
        this.usuarioNombre = usuarioNombre;
    }

    // Getters y setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getRecibidoEn() {
        return recibidoEn;
    }

    public void setRecibidoEn(OffsetDateTime recibidoEn) {
        this.recibidoEn = recibidoEn;
    }

    public boolean isConciliado() {
        return conciliado;
    }

    public void setConciliado(boolean conciliado) {
        this.conciliado = conciliado;
    }

    public UUID getPagoId() {
        return pagoId;
    }

    public void setPagoId(UUID pagoId) {
        this.pagoId = pagoId;
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
}
