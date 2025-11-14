package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaBiometricaRepository extends JpaRepository<PlantillaBiometrica, UUID> {
	List<PlantillaBiometrica> findByUsuarioIdAndActivoTrue(UUID usuarioId);

	List<PlantillaBiometrica> findByUsuario(Usuario usuario);

	List<PlantillaBiometrica> findByUsuarioAndModalidad(Usuario usuario, Modalidad modalidad);
}
