package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.SesionWebAuthn;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SesionWebAuthnRepository extends JpaRepository<SesionWebAuthn, UUID> {
    
    Optional<SesionWebAuthn> findByIdAndEstado(UUID id, String estado);
    
    @Query("SELECT s FROM SesionWebAuthn s WHERE s.expiraEn < :now AND s.estado = 'PENDIENTE'")
    List<SesionWebAuthn> findExpiradas(OffsetDateTime now);
}
