package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.CredencialWebAuthn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredencialWebAuthnRepository extends JpaRepository<CredencialWebAuthn, UUID> {
    
    Optional<CredencialWebAuthn> findByCredentialId(String credentialId);
    
    List<CredencialWebAuthn> findByUsuarioIdAndActivoTrue(UUID usuarioId);
}
