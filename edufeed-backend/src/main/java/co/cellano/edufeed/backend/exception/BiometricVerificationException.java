package co.cellano.edufeed.backend.exception;

/**
 * Excepción lanzada cuando falla el proceso de verificación biométrica.
 */
public class BiometricVerificationException extends RuntimeException {
    private final String modalidad;
    private final String reason;

    public BiometricVerificationException(String modalidad, String reason) {
        super(String.format("Error al verificar modalidad %s: %s", modalidad, reason));
        this.modalidad = modalidad;
        this.reason = reason;
    }

    public BiometricVerificationException(String modalidad, String reason, Throwable cause) {
        super(String.format("Error al verificar modalidad %s: %s", modalidad, reason), cause);
        this.modalidad = modalidad;
        this.reason = reason;
    }

    public String getModalidad() {
        return modalidad;
    }

    public String getReason() {
        return reason;
    }
}
