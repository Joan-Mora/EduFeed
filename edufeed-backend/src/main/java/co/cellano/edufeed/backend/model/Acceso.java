package co.cellano.edufeed.backend.model;

import co.cellano.edufeed.backend.model.enums.EstadoAcceso;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accesos")
public class Acceso {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAcceso estado;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 20)
    private Modalidad modalidad;

    @Column(name = "metadatos_coincidencia", columnDefinition = "jsonb")
    private String metadatosCoincidencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derecho_id")
    private DerechoUso derecho;

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

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(OffsetDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public EstadoAcceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoAcceso estado) {
        this.estado = estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public String getMetadatosCoincidencia() {
        return metadatosCoincidencia;
    }

    public void setMetadatosCoincidencia(String metadatosCoincidencia) {
        this.metadatosCoincidencia = metadatosCoincidencia;
    }

    public DerechoUso getDerecho() {
        return derecho;
    }

    public void setDerecho(DerechoUso derecho) {
        this.derecho = derecho;
    }
}
