package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlantillaBiometricaRepository extends JpaRepository<PlantillaBiometrica, UUID> {
}
