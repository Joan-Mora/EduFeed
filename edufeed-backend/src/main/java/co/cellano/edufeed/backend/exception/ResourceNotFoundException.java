package co.cellano.edufeed.backend.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra un recurso solicitado.
 */
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final Object identifier;

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(String.format("%s no encontrado con id: %s", resourceType, id));
        this.resourceType = resourceType;
        this.identifier = id;
    }

    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(String.format("%s no encontrado con %s: %s", resourceType, field, value));
        this.resourceType = resourceType;
        this.identifier = value;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
