package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.UsuarioRol;
import co.cellano.edufeed.backend.model.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
}
