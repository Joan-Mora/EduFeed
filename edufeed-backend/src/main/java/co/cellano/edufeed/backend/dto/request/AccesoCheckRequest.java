package co.cellano.edufeed.backend.dto.request;

import co.cellano.edufeed.backend.model.enums.Modalidad;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para solicitud de verificación de acceso.
 * Usado en el endpoint POST /api/accesos/verificar
 */
public class AccesoCheckRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private UUID usuarioId;

    @NotNull(message = "La modalidad biométrica es obligatoria")
    private Modalidad modalidad;

    public AccesoCheckRequest() {
    }

    public AccesoCheckRequest(UUID usuarioId, Modalidad modalidad) {
        this.usuarioId = usuarioId;
        this.modalidad = modalidad;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }
}
