package co.cellano.edufeed.backend.exception;

/**
 * Excepción lanzada cuando un pago viola reglas de negocio específicas.
 * <p>
 * Ejemplos:
 * - Monto <= 0
 * - Vigencias incoherentes (vigente_hasta < vigente_desde)
 * - Paquete sin días especificados
 * - Tipo de pago inválido para la operación
 * </p>
 * 
 * HTTP Status: 400 Bad Request
 * 
 * @since FASE 2.2
 */
public class InvalidPaymentException extends RuntimeException {
    private final String ruleCode;

    public InvalidPaymentException(String message) {
        super(message);
        this.ruleCode = null;
    }

    public InvalidPaymentException(String message, String ruleCode) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
