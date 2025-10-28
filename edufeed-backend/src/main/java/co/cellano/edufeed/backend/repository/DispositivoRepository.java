package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Dispositivo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {
}
