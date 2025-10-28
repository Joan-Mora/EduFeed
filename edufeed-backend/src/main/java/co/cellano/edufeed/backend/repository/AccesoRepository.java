package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Acceso;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio para gestión de accesos.
 * Soporta consultas dinámicas con Specification para filtros complejos.
 * 
 * @since FASE 1, extendido en FASE 2.3, FASE 3.3
 */
public interface AccesoRepository extends JpaRepository<Acceso, UUID>, JpaSpecificationExecutor<Acceso> {

    /**
     * Agrega asistencias diarias (accesos exitosos) en el periodo especificado.
     * Retorna: [dia, total_accesos, usuarios_unicos]
     * 
     * @since FASE 3.3
     */
    @Query(value = """
            SELECT 
                DATE(a.creado_en AT TIME ZONE 'America/Bogota') as dia,
                COUNT(*) as total_accesos,
                COUNT(DISTINCT a.usuario_id) as usuarios_unicos
            FROM accesos a
            WHERE a.exitoso = true
                AND (:desde IS NULL OR a.creado_en >= :desde)
                AND (:hasta IS NULL OR a.creado_en <= :hasta)
            GROUP BY DATE(a.creado_en AT TIME ZONE 'America/Bogota')
            ORDER BY dia DESC
            """, nativeQuery = true)
    List<Object[]> aggregateAsistenciasDiarias(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);

    /**
     * Agrega rechazos diarios (accesos fallidos) por motivo en el periodo especificado.
     * Retorna: [dia, motivo_rechazo, cantidad]
     * 
     * @since FASE 3.3
     */
    @Query(value = """
            SELECT 
                DATE(a.creado_en AT TIME ZONE 'America/Bogota') as dia,
                COALESCE(a.motivo_rechazo, 'SIN_ESPECIFICAR') as motivo_rechazo,
                COUNT(*) as cantidad
            FROM accesos a
            WHERE a.exitoso = false
                AND (:desde IS NULL OR a.creado_en >= :desde)
                AND (:hasta IS NULL OR a.creado_en <= :hasta)
            GROUP BY DATE(a.creado_en AT TIME ZONE 'America/Bogota'), COALESCE(a.motivo_rechazo, 'SIN_ESPECIFICAR')
            ORDER BY dia DESC, cantidad DESC
            """, nativeQuery = true)
    List<Object[]> aggregateRechazosDiarios(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);
}
