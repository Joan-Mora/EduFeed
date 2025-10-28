package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.PaquetePago;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio para gestión de paquetes de pago.
 * 
 * @since FASE 1, extendido en FASE 2.2
 */
public interface PaquetePagoRepository extends JpaRepository<PaquetePago, UUID> {

    /**
     * Busca un paquete por el ID del pago asociado.
     */
    Optional<PaquetePago> findByPagoId(UUID pagoId);
}
