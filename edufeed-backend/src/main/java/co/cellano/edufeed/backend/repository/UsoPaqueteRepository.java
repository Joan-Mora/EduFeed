package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.UsoPaquete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsoPaqueteRepository extends JpaRepository<UsoPaquete, UUID> {
}
