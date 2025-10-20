package co.cellano.edufeed.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
public class Auditoria {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tipo_entidad", nullable = false, length = 50)
    private String tipoEntidad;

    @Column(name = "entidad_id")
    private UUID entidadId;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion; // CREATE | UPDATE | DELETE

    @Column(name = "realizado_por", length = 200)
    private String realizadoPor;

    @Column(name = "realizado_en", nullable = false)
    private OffsetDateTime realizadoEn = OffsetDateTime.now();

    @Column(name = "valores_anteriores", columnDefinition = "jsonb")
    private String valoresAnteriores;

    @Column(name = "valores_nuevos", columnDefinition = "jsonb")
    private String valoresNuevos;

    @Column(name = "reason")
    private String reason;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipoEntidad() {
        return tipoEntidad;
    }

    public void setTipoEntidad(String tipoEntidad) {
        this.tipoEntidad = tipoEntidad;
    }

    public UUID getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(UUID entidadId) {
        this.entidadId = entidadId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getRealizadoPor() {
        return realizadoPor;
    }

    public void setRealizadoPor(String realizadoPor) {
        this.realizadoPor = realizadoPor;
    }

    public OffsetDateTime getRealizadoEn() {
        return realizadoEn;
    }

    public void setRealizadoEn(OffsetDateTime realizadoEn) {
        this.realizadoEn = realizadoEn;
    }

    public String getValoresAnteriores() {
        return valoresAnteriores;
    }

    public void setValoresAnteriores(String valoresAnteriores) {
        this.valoresAnteriores = valoresAnteriores;
    }

    public String getValoresNuevos() {
        return valoresNuevos;
    }

    public void setValoresNuevos(String valoresNuevos) {
        this.valoresNuevos = valoresNuevos;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
