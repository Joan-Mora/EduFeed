package co.cellano.edufeed.backend.model;

import co.cellano.edufeed.backend.audit.AuditListener;
import co.cellano.edufeed.backend.audit.Auditable;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "derechos_uso")
@EntityListeners(AuditListener.class)
public class DerechoUso implements Auditable {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_derecho", nullable = false, length = 20)
    private TipoPago tipoDerecho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_origen_id")
    private Pago pagoOrigen;

    @Column(name = "vigente_desde", nullable = false)
    private OffsetDateTime vigenteDesde;

    @Column(name = "vigente_hasta")
    private OffsetDateTime vigenteHasta;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

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

    public TipoPago getTipoDerecho() {
        return tipoDerecho;
    }

    public void setTipoDerecho(TipoPago tipoDerecho) {
        this.tipoDerecho = tipoDerecho;
    }

    public Pago getPagoOrigen() {
        return pagoOrigen;
    }

    public void setPagoOrigen(Pago pagoOrigen) {
        this.pagoOrigen = pagoOrigen;
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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public String getEntityName() {
        return "DerechoUso";
    }
}
