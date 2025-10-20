package co.cellano.edufeed.backend.audit;

import co.cellano.edufeed.backend.model.Auditoria;
import co.cellano.edufeed.backend.repository.AuditoriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditoriaRepository auditoriaRepository, ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
    }

    public void auditarCreacion(Object entity) {
        if (!(entity instanceof Auditable a))
            return;
        Auditoria audit = base(a);
        audit.setAccion("CREATE");
        audit.setValoresNuevos(serialize(entity));
        auditoriaRepository.save(audit);
    }

    public void auditarActualizacion(Object entity) {
        if (!(entity instanceof Auditable a))
            return;
        Auditoria audit = base(a);
        audit.setAccion("UPDATE");
        // Para primera versión simple, solo registramos nuevos.
        audit.setValoresNuevos(serialize(entity));
        auditoriaRepository.save(audit);
    }

    public void auditarEliminacion(Object entity) {
        if (!(entity instanceof Auditable a))
            return;
        Auditoria audit = base(a);
        audit.setAccion("DELETE");
        audit.setValoresAnteriores(serialize(entity));
        auditoriaRepository.save(audit);
    }

    private Auditoria base(Auditable a) {
        Auditoria audit = new Auditoria();
        audit.setTipoEntidad(a.getEntityName());
        UUID id = a.getId();
        audit.setEntidadId(id);
        audit.setRealizadoPor(AuditContext.getCurrentActor());
        audit.setRealizadoEn(OffsetDateTime.now());
        return audit;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}"; // fallback
        }
    }
}
