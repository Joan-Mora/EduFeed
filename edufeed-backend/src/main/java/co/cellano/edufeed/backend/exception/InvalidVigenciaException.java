package co.cellano.edufeed.backend.exception;

/**
 * Excepción lanzada cuando las vigencias son inválidas o incoherentes.
 * <p>
 * Ejemplos:
 * - vigente_hasta anterior a vigente_desde
 * - Vigencias fuera del rango permitido
 * - Intento de generar derecho con pago sin vigencias válidas
 * </p>
 * 
 * HTTP Status: 400 Bad Request
 * 
 * @since FASE 2.2
 */
public class InvalidVigenciaException extends RuntimeException {
    private final String ruleCode;

    public InvalidVigenciaException(String message) {
        super(message);
        this.ruleCode = null;
    }

    public InvalidVigenciaException(String message, String ruleCode) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
