package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByDocumento(String documento);
}
