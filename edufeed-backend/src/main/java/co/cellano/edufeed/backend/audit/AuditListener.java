package co.cellano.edufeed.backend.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditListener {

    private static AuditService auditService;

    @Autowired
    public void setAuditService(AuditService service) {
        AuditListener.auditService = service;
    }

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof Auditable && auditService != null) {
            auditService.auditarCreacion(entity);
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof Auditable && auditService != null) {
            auditService.auditarActualizacion(entity);
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        if (entity instanceof Auditable && auditService != null) {
            auditService.auditarEliminacion(entity);
        }
    }
}
