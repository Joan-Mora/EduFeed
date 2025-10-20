package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.exception.InvalidPaymentException;
import co.cellano.edufeed.backend.exception.InvalidVigenciaException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.DerechoUso;
import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.repository.DerechoUsoRepository;
import co.cellano.edufeed.backend.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestión de derechos de uso.
 * 
 * <p>
 * Responsabilidades:
 * <ul>
 * <li>Generar derechos de uso a partir de pagos aprobados</li>
 * <li>Calcular vigencias automáticamente según tipo de pago</li>
 * <li>Gestionar derechos activos por usuario</li>
 * <li>Coordinar con PaquetePagoService para paquetes</li>
 * </ul>
 * </p>
 * 
 * @since FASE 2.2
 */
@Service
@Transactional
public class DerechoUsoService {
    private static final Logger log = LoggerFactory.getLogger(DerechoUsoService.class);

    private final DerechoUsoRepository derechoUsoRepository;
    private final PagoRepository pagoRepository;
    private final PaquetePagoService paquetePagoService;
    private final ZoneId timezone;

    public DerechoUsoService(DerechoUsoRepository derechoUsoRepository,
            PagoRepository pagoRepository,
            PaquetePagoService paquetePagoService) {
        this.derechoUsoRepository = derechoUsoRepository;
        this.pagoRepository = pagoRepository;
        this.paquetePagoService = paquetePagoService;
        this.timezone = ZoneId.of("America/Bogota");
    }

    /**
     * Genera un derecho de uso a partir de un pago aprobado.
     * 
     * <p>
     * Lógica de vigencias:
     * <ul>
     * <li>DIARIO: Copia vigencias del pago (solo hoy)</li>
     * <li>MENSUAL: Copia vigencias del pago (primer-último día mes)</li>
     * <li>PAQUETE: Genera vigencia de 24h y consume 1 día del paquete</li>
     * </ul>
     * </p>
     * 
     * @param pagoId ID del pago aprobado
     * @return DerechoUso generado
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidPaymentException   si el pago no está aprobado
     * @throws InvalidVigenciaException  si las vigencias son inválidas
     */
    public DerechoUso generarDerecho(UUID pagoId) {
        log.debug("Generando derecho de uso para pago {}", pagoId);

        // Validar que el pago existe
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", pagoId));

        // Validar que el pago está aprobado
        if (pago.getEstadoPago() != EstadoPago.APROBADO) {
            throw new InvalidPaymentException(
                    "Solo se pueden generar derechos de pagos APROBADOS. Estado actual: " + pago.getEstadoPago(),
                    "PAGO_NO_APROBADO");
        }

        // Crear derecho de uso
        DerechoUso derecho = new DerechoUso();
        derecho.setUsuario(pago.getUsuario());
        derecho.setTipoDerecho(pago.getTipoPago());
        derecho.setPagoOrigen(pago);
        derecho.setActivo(true);

        // Calcular vigencias según tipo de pago
        calcularVigenciasDerechoUso(derecho, pago);

        // Guardar derecho
        DerechoUso saved = derechoUsoRepository.save(derecho);

        log.info("Derecho de uso generado: id={}, usuario={}, tipo={}, vigencia {}-{}",
                saved.getId(),
                saved.getUsuario().getDocumento(),
                saved.getTipoDerecho(),
                saved.getVigenteDesde(),
                saved.getVigenteHasta());

        return saved;
    }

    /**
     * Calcula vigencias del derecho de uso según el tipo de pago.
     */
    private void calcularVigenciasDerechoUso(DerechoUso derecho, Pago pago) {
        OffsetDateTime ahora = OffsetDateTime.now(timezone);

        switch (pago.getTipoPago()) {
            case DIARIO -> {
                // Copiar vigencias del pago
                if (pago.getVigenteDesde() == null || pago.getVigenteHasta() == null) {
                    throw new InvalidVigenciaException(
                            "Pago DIARIO debe tener vigencias definidas",
                            "VIGENCIAS_DIARIO_FALTANTES");
                }
                derecho.setVigenteDesde(pago.getVigenteDesde());
                derecho.setVigenteHasta(pago.getVigenteHasta());
                log.debug("Derecho DIARIO: vigencia {} - {}", derecho.getVigenteDesde(), derecho.getVigenteHasta());
            }
            case MENSUAL -> {
                // Copiar vigencias del pago
                if (pago.getVigenteDesde() == null || pago.getVigenteHasta() == null) {
                    throw new InvalidVigenciaException(
                            "Pago MENSUAL debe tener vigencias definidas",
                            "VIGENCIAS_MENSUAL_FALTANTES");
                }
                derecho.setVigenteDesde(pago.getVigenteDesde());
                derecho.setVigenteHasta(pago.getVigenteHasta());
                log.debug("Derecho MENSUAL: vigencia {} - {}", derecho.getVigenteDesde(), derecho.getVigenteHasta());
            }
            case PAQUETE -> {
                // Consumir un día del paquete
                paquetePagoService.consumirDia(pago.getId());

                // Generar vigencia de 24 horas desde ahora
                OffsetDateTime inicioDia = ahora.toLocalDate().atStartOfDay(timezone).toOffsetDateTime();
                OffsetDateTime finDia = inicioDia.plusDays(1).minusNanos(1);
                derecho.setVigenteDesde(inicioDia);
                derecho.setVigenteHasta(finDia);
                log.debug("Derecho PAQUETE: vigencia {} - {} (día consumido)",
                        derecho.getVigenteDesde(), derecho.getVigenteHasta());
            }
        }

        // Validar coherencia de vigencias
        if (derecho.getVigenteDesde() != null && derecho.getVigenteHasta() != null) {
            if (derecho.getVigenteHasta().isBefore(derecho.getVigenteDesde())) {
                throw new InvalidVigenciaException(
                        "vigente_hasta no puede ser anterior a vigente_desde",
                        "VIGENCIAS_INCOHERENTES");
            }
        }
    }

    /**
     * Obtiene derechos activos de un usuario.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de derechos activos y vigentes
     */
    @Transactional(readOnly = true)
    public List<DerechoUso> obtenerDerechosActivos(UUID usuarioId) {
        OffsetDateTime ahora = OffsetDateTime.now(timezone);
        return derechoUsoRepository.findByUsuarioIdAndActivoTrueAndVigenteHastaAfter(usuarioId, ahora);
    }

    /**
     * Verifica si un usuario tiene al menos un derecho activo y vigente.
     * 
     * @param usuarioId ID del usuario
     * @return true si tiene derecho activo, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean tieneDerechoActivo(UUID usuarioId) {
        return !obtenerDerechosActivos(usuarioId).isEmpty();
    }

    /**
     * Desactiva un derecho de uso.
     * 
     * @param derechoId ID del derecho
     */
    public void desactivarDerecho(UUID derechoId) {
        log.debug("Desactivando derecho {}", derechoId);

        DerechoUso derecho = derechoUsoRepository.findById(derechoId)
                .orElseThrow(() -> new ResourceNotFoundException("DerechoUso", derechoId));

        derecho.setActivo(false);
        derechoUsoRepository.save(derecho);

        log.info("Derecho desactivado: id={}", derechoId);
    }

    /**
     * Lista todos los derechos de un usuario.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de todos los derechos (activos e inactivos)
     */
    @Transactional(readOnly = true)
    public List<DerechoUso> listarDerechosPorUsuario(UUID usuarioId) {
        return derechoUsoRepository.findByUsuarioId(usuarioId);
    }
}
