package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.TransaccionCaja;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransaccionCajaRepository extends JpaRepository<TransaccionCaja, UUID> {

    /**
     * Busca transacciones por proveedor.
     */
    List<TransaccionCaja> findByProveedorOrderByRecibidoEnDesc(String proveedor);

    /**
     * Busca transacciones por referencia externa.
     */
    Optional<TransaccionCaja> findByReferenciaExterna(String referenciaExterna);

    /**
     * Busca transacciones no conciliadas.
     */
    @Query("SELECT t FROM TransaccionCaja t WHERE t.conciliado = false ORDER BY t.recibidoEn DESC")
    List<TransaccionCaja> findNoConciliadas();

    /**
     * Busca transacciones con paginación.
     */
    Page<TransaccionCaja> findAllByOrderByRecibidoEnDesc(Pageable pageable);

    /**
     * Busca transacciones por rango de fechas.
     */
    @Query("SELECT t FROM TransaccionCaja t WHERE t.recibidoEn BETWEEN :desde AND :hasta ORDER BY t.recibidoEn DESC")
    List<TransaccionCaja> findByRangoFechas(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    /**
     * Busca transacciones por estado.
     */
    List<TransaccionCaja> findByEstadoOrderByRecibidoEnDesc(String estado);

    /**
     * Busca transacciones conciliadas con detalles del pago y usuario.
     */
    @Query("""
        SELECT t.id, t.proveedor, t.referenciaExterna, t.monto, t.metodoPago, 
               t.estado, t.recibidoEn, t.conciliado,
               p.id, u.documento, u.nombreCompleto
        FROM TransaccionCaja t
        LEFT JOIN t.pago p
        LEFT JOIN p.usuario u
        WHERE t.conciliado = :conciliado
        ORDER BY t.recibidoEn DESC
        """)
    List<Object[]> findConDetallesByConciliado(@Param("conciliado") boolean conciliado);

    /**
     * Cuenta transacciones no conciliadas.
     */
    @Query("SELECT COUNT(t) FROM TransaccionCaja t WHERE t.conciliado = false")
    long countNoConciliadas();

    /**
     * Busca transacciones por estado de conciliación con paginación.
     */
    Page<TransaccionCaja> findByConciliadoOrderByRecibidoEnDesc(boolean conciliado, Pageable pageable);
}
