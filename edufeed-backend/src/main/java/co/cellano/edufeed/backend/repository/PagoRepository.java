package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para gestión de pagos.
 * 
 * @since FASE 1, extendido en FASE 2.2
 */
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    /**
     * Busca pagos por usuario.
     */
    List<Pago> findByUsuarioId(UUID usuarioId);

    /**
     * Busca pagos por tipo.
     */
    List<Pago> findByTipoPago(TipoPago tipoPago);

    /**
     * Busca pagos por estado.
     */
    List<Pago> findByEstadoPago(EstadoPago estadoPago);

    /**
     * Busca pagos en rango de fechas de creación.
     */
    List<Pago> findByCreadoEnBetween(OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Busca pagos por usuario y estado.
     */
    List<Pago> findByUsuarioIdAndEstadoPago(UUID usuarioId, EstadoPago estadoPago);

    /**
     * Busca pagos por usuario y tipo.
     */
    List<Pago> findByUsuarioIdAndTipoPago(UUID usuarioId, TipoPago tipoPago);
}
