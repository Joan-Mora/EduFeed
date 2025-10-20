package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {
}
