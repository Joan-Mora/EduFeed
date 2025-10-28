package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.UsoPaquete;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsoPaqueteRepository extends JpaRepository<UsoPaquete, UUID> {
}
