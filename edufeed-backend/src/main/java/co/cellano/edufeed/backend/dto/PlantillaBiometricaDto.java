package co.cellano.edufeed.backend.dto;

import co.cellano.edufeed.backend.model.enums.Modalidad;
import java.time.OffsetDateTime;

public class PlantillaBiometricaDto {
    private String id;
    private Modalidad modalidad;
    private String proveedor;
    private OffsetDateTime creadoEn;
    private Boolean activo;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
