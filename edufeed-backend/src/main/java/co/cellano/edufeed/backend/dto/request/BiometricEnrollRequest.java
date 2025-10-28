package co.cellano.edufeed.backend.dto.request;

import co.cellano.edufeed.backend.model.enums.Modalidad;
import jakarta.validation.constraints.NotNull;

public class BiometricEnrollRequest {
    @NotNull
    private Modalidad modalidad;

    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }
}
