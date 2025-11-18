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
     * Trae todas las transacciones de un proveedor específico.
     */
    List<TransaccionCaja> findByProveedorOrderByRecibidoEnDesc(String proveedor);

    /**
     * Encuentra una transacción usando su referencia externa.
     */
    Optional<TransaccionCaja> findByReferenciaExterna(String referenciaExterna);

    /**
     * Trae las transacciones que todavía no han sido conciliadas.
     */
    @Query("SELECT t FROM TransaccionCaja t WHERE t.conciliado = false ORDER BY t.recibidoEn DESC")
    List<TransaccionCaja> findNoConciliadas();

    /**
     * Lista todas las transacciones con soporte para paginación.
     */
    Page<TransaccionCaja> findAllByOrderByRecibidoEnDesc(Pageable pageable);

    /**
     * Filtra transacciones entre dos fechas específicas.
     */
    @Query("SELECT t FROM TransaccionCaja t WHERE t.recibidoEn BETWEEN :desde AND :hasta ORDER BY t.recibidoEn DESC")
    List<TransaccionCaja> findByRangoFechas(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);

    /**
     * Obtiene transacciones según su estado.
     */
    List<TransaccionCaja> findByEstadoOrderByRecibidoEnDesc(String estado);

    /**
     * Trae transacciones con toda la info del pago y usuario incluida.
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
     * Cuenta cuántas transacciones están sin conciliar.
     */
    @Query("SELECT COUNT(t) FROM TransaccionCaja t WHERE t.conciliado = false")
    long countNoConciliadas();

    /**
     * Filtra transacciones según si están conciliadas o no, con paginación.
     */
    Page<TransaccionCaja> findByConciliadoOrderByRecibidoEnDesc(boolean conciliado, Pageable pageable);
}
