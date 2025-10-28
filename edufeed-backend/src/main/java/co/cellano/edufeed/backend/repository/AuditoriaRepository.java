package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Auditoria;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {
}
