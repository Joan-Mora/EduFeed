package co.cellano.edufeed.backend.exception;

/**
 * Excepción lanzada cuando se intenta crear un usuario con un documento ya
 * existente.
 */
public class DuplicateDocumentException extends RuntimeException {
    private final String documento;

    public DuplicateDocumentException(String documento) {
        super("Ya existe un usuario con el documento: " + documento);
        this.documento = documento;
    }

    public String getDocumento() {
        return documento;
    }
}
