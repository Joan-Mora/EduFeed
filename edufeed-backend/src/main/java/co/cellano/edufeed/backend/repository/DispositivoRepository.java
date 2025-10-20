package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {
}
