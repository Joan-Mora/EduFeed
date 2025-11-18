package co.cellano.edufeed.backend.exception;

public class BiometricEnrollmentException extends RuntimeException {
    private final String userId;
    private final String modalidad;

    public BiometricEnrollmentException(String userId, String modalidad, String message) {
        super(String.format("Error al enrolar usuario %s con modalidad %s: %s", userId, modalidad, message));
        this.userId = userId;
        this.modalidad = modalidad;
    }

    public BiometricEnrollmentException(String userId, String modalidad, String message, Throwable cause) {
        super(String.format("Error al enrolar usuario %s con modalidad %s: %s", userId, modalidad, message), cause);
        this.userId = userId;
        this.modalidad = modalidad;
    }

    public String getUserId() {
        return userId;
    }

    public String getModalidad() {
        return modalidad;
    }
}
