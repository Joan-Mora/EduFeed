package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.domain.Operador;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperadorRepository extends JpaRepository<Operador, UUID> {
    Optional<Operador> findByUsername(String username);
}
