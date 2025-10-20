package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.DerechoUso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para gestión de derechos de uso.
 * 
 * @since FASE 1, extendido en FASE 2.2 y FASE 2.3
 */
public interface DerechoUsoRepository extends JpaRepository<DerechoUso, UUID> {

    /**
     * Busca derechos de un usuario.
     */
    List<DerechoUso> findByUsuarioId(UUID usuarioId);

    /**
     * Busca derechos activos y vigentes de un usuario.
     * Un derecho es vigente si vigente_hasta es posterior a la fecha actual.
     */
    List<DerechoUso> findByUsuarioIdAndActivoTrueAndVigenteHastaAfter(UUID usuarioId, OffsetDateTime ahora);

    /**
     * Busca derechos por pago origen.
     */
    List<DerechoUso> findByPagoOrigenId(UUID pagoId);

    /**
     * Busca el derecho vigente de un usuario en un momento específico.
     * Un derecho es vigente si el momento actual está entre vigente_desde y
     * vigente_hasta.
     * 
     * @param usuarioId ID del usuario
     * @param ahora     Momento a verificar
     * @return Derecho vigente si existe
     * @since FASE 2.3
     */
    @Query("SELECT d FROM DerechoUso d WHERE d.usuario.id = :usuarioId " +
            "AND :ahora BETWEEN d.vigenteDesde AND d.vigenteHasta " +
            "ORDER BY d.creadoEn DESC")
    Optional<DerechoUso> findDerechoVigente(@Param("usuarioId") UUID usuarioId,
            @Param("ahora") OffsetDateTime ahora);
}
