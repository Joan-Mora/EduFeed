package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Busca un pago por referencia externa (usado para conciliación con transacciones de caja).
     */
    Optional<Pago> findByReferenciaExterna(String referenciaExterna);

    /**
     * Agregación de ingresos diarios por tipo y método de pago.
     * Filtra por rango de fechas en creado_en y excluye pagos RECHAZADO.
     * Retorna filas: dia (date), tipo_pago (text), metodo_pago (text), cantidad (bigint), total (numeric)
     */
    @Query(value = "SELECT date_trunc('day', p.creado_en)::date AS dia, p.tipo_pago, COALESCE(p.metodo_pago, 'DESCONOCIDO') AS metodo_pago, COUNT(*) AS cantidad, SUM(p.monto) AS total\n" +
            "FROM pagos p\n" +
            "WHERE (:desde IS NULL OR p.creado_en >= :desde)\n" +
            "  AND (:hasta IS NULL OR p.creado_en <= :hasta)\n" +
            "  AND (p.estado_pago IS NULL OR p.estado_pago <> 'RECHAZADO')\n" +
            "GROUP BY 1,2,3\n" +
            "ORDER BY 1", nativeQuery = true)
    List<Object[]> aggregateIngresosDiarios(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);
}
