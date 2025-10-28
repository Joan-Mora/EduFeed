package co.cellano.edufeed.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload estándar para webhook de pagos provenientes de caja/POS.
 */
public class WebhookPagoRequest {
    @NotBlank
    private String proveedor;

    private String referenciaExterna;

    @NotNull
    private BigDecimal monto;

    private String metodoPago;

    /** Estado reportado por el proveedor: PENDIENTE | APROBADO | RECHAZADO | ANULADO */
    private String estado;

    /** JSON crudo del proveedor (stringificado) para trazabilidad */
    private String payload;

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
