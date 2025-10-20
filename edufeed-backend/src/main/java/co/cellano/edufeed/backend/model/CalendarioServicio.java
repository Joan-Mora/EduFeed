package co.cellano.edufeed.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "calendario_servicio")
public class CalendarioServicio {
    @Id
    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "observacion")
    private String observacion;

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
