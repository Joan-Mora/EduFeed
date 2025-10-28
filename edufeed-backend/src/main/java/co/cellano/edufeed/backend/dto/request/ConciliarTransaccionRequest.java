package co.cellano.edufeed.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request para conciliación manual de transacciones con pagos.
 */
public class ConciliarTransaccionRequest {
    @NotNull(message = "El ID del pago es obligatorio")
    private UUID pagoId;

    public UUID getPagoId() {
        return pagoId;
    }

    public void setPagoId(UUID pagoId) {
        this.pagoId = pagoId;
    }
}
