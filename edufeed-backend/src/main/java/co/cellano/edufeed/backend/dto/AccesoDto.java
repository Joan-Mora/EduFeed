package co.cellano.edufeed.backend.dto;

import co.cellano.edufeed.backend.model.enums.EstadoAcceso;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public class AccesoDto {
    private String id;

    private String usuarioId;

    private String derechoId;

    @NotNull
    private EstadoAcceso estado;

    private Modalidad modalidad;

    @Size(max = 200)
    private String motivo;

    private OffsetDateTime fechaHora;

    private String metadatosCoincidenciaJson;

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

    public String getDerechoId() {
        return derechoId;
    }

    public void setDerechoId(String derechoId) {
        this.derechoId = derechoId;
    }

    public EstadoAcceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoAcceso estado) {
        this.estado = estado;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(OffsetDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getMetadatosCoincidenciaJson() {
        return metadatosCoincidenciaJson;
    }

    public void setMetadatosCoincidenciaJson(String metadatosCoincidenciaJson) {
        this.metadatosCoincidenciaJson = metadatosCoincidenciaJson;
    }
}
