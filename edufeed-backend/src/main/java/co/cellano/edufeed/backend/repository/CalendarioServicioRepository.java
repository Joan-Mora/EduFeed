package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.CalendarioServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface CalendarioServicioRepository extends JpaRepository<CalendarioServicio, LocalDate> {
}
