package co.cellano.edufeed.backend.exception;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 */
public class InvalidBusinessRuleException extends RuntimeException {
    private final String ruleCode;

    public InvalidBusinessRuleException(String message) {
        super(message);
        this.ruleCode = null;
    }

    public InvalidBusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
