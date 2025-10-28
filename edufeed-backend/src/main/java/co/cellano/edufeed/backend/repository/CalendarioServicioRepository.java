package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.CalendarioServicio;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarioServicioRepository extends JpaRepository<CalendarioServicio, LocalDate> {
}
