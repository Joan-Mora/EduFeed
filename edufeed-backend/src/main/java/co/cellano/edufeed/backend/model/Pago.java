package co.cellano.edufeed.backend.model;

import co.cellano.edufeed.backend.audit.AuditListener;
import co.cellano.edufeed.backend.audit.Auditable;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pagos")
@EntityListeners(AuditListener.class)
public class Pago implements Auditable {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 20)
    private TipoPago tipoPago;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now(ZoneId.of("America/Bogota"));

    @Column(name = "vigente_desde")
    private OffsetDateTime vigenteDesde;

    @Column(name = "vigente_hasta")
    private OffsetDateTime vigenteHasta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadatos", columnDefinition = "jsonb")
    private String metadatos;

    @Column(name = "metodo_pago", length = 20)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", length = 20)
    private EstadoPago estadoPago;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "cajero", length = 100)
    private String cajero;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public String getMetadatos() {
        return metadatos;
    }

    public void setMetadatos(String metadatos) {
        this.metadatos = metadatos;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
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

    @Override
    public String getEntityName() {
        return "Pago";
    }
}
