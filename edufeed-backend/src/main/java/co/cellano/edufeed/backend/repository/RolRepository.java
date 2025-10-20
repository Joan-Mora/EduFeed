package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {
}
