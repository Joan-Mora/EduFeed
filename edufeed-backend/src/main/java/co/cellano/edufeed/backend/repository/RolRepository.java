package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Rol;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, UUID> {
}
