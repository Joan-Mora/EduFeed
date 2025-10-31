package co.cellano.edufeed.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transacciones_caja")
public class TransaccionCaja {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "proveedor", length = 60)
    private String proveedor;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", length = 20)
    private String metodoPago;

    @Column(name = "estado", length = 20)
    private String estado; // PENDIENTE | APROBADO | RECHAZADO | ANULADO

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "recibido_en", nullable = false)
    private OffsetDateTime recibidoEn = OffsetDateTime.now();

    @Column(name = "conciliado", nullable = false)
    private boolean conciliado = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
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

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }
}
