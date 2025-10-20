package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.CredencialWebAuthn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CredencialWebAuthnRepository extends JpaRepository<CredencialWebAuthn, UUID> {
}
