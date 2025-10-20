package co.cellano.edufeed.backend.exception;

/**
 * Excepción lanzada cuando un paquete de pago no tiene días suficientes.
 * <p>
 * Ejemplos:
 * - Intento de consumir días de un paquete agotado (dias_restantes = 0)
 * - Intento de generar derecho de uso de un paquete sin días disponibles
 * </p>
 * 
 * HTTP Status: 400 Bad Request
 * 
 * @since FASE 2.2
 */
public class InsufficientPackageException extends RuntimeException {
    private final int diasRestantes;

    public InsufficientPackageException(String message, int diasRestantes) {
        super(message);
        this.diasRestantes = diasRestantes;
    }

    public int getDiasRestantes() {
        return diasRestantes;
    }
}
