package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaBiometricaRepository extends JpaRepository<PlantillaBiometrica, UUID> {
	List<PlantillaBiometrica> findByUsuarioIdAndActivoTrue(UUID usuarioId);
}
