package co.cellano.edufeed.backend.model;

import co.cellano.edufeed.backend.model.enums.Modalidad;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plantillas_biometricas")
public class PlantillaBiometrica {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "proveedor", length = 100)
    private String proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 20)
    private Modalidad modalidad;

    // PostgreSQL: usar bytea en vez de OID (LOB)
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "plantilla", nullable = false, columnDefinition = "bytea")
    private byte[] plantilla;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // Constructores (POO)
    public PlantillaBiometrica() {}

    public PlantillaBiometrica(Usuario usuario, String proveedor, Modalidad modalidad, byte[] plantilla) {
        this.usuario = usuario;
        this.proveedor = proveedor;
        this.modalidad = modalidad;
        this.plantilla = plantilla;
    }

    public PlantillaBiometrica(UUID id, Usuario usuario, String proveedor, Modalidad modalidad,
                               byte[] plantilla, OffsetDateTime creadoEn, boolean activo) {
        this.id = id;
        this.usuario = usuario;
        this.proveedor = proveedor;
        this.modalidad = modalidad;
        this.plantilla = plantilla;
        this.creadoEn = creadoEn;
        this.activo = activo;
    }

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

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public byte[] getPlantilla() {
        return plantilla;
    }

    public void setPlantilla(byte[] plantilla) {
        this.plantilla = plantilla;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
