package co.cellano.edufeed.backend.audit;

import java.util.UUID;

/**
 * Interface marker para entidades que deben ser auditadas automáticamente.
 */
public interface Auditable {
    UUID getId();

    String getEntityName();
}
