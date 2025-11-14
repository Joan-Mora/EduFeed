package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.exception.InvalidPaymentException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.mapper.PagoMapper;
import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.PaquetePago;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.repository.PagoRepository;
import co.cellano.edufeed.backend.repository.DerechoUsoRepository;
import co.cellano.edufeed.backend.repository.PaquetePagoRepository;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de pagos con lógica de tipos de pago.
 * 
 * <p>
 * Tipos de pago soportados:
 * <ul>
 * <li>DIARIO: Vigencia solo para el día actual</li>
 * <li>MENSUAL: Vigencia del primer al último día del mes actual</li>
 * <li>PAQUETE: Crea entrada en paquetes_pago con días disponibles</li>
 * </ul>
 * </p>
 * 
 * @since FASE 2.2
 */
@Service
@Transactional
public class PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaquetePagoRepository paquetePagoRepository;
    private final PaquetePagoService paquetePagoService;
    private final DerechoUsoRepository derechoUsoRepository;
    private final DerechoUsoService derechoUsoService;
    private final ZoneId timezone;

    public PagoService(PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository,
            PaquetePagoRepository paquetePagoRepository,
            DerechoUsoService derechoUsoService,
            PaquetePagoService paquetePagoService,
            DerechoUsoRepository derechoUsoRepository) {
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.paquetePagoRepository = paquetePagoRepository;
        this.derechoUsoService = derechoUsoService;
        this.paquetePagoService = paquetePagoService;
        this.derechoUsoRepository = derechoUsoRepository;
        this.timezone = ZoneId.of("America/Bogota");
    }

    /**
     * Crea un nuevo pago con validaciones y cálculo automático de vigencias.
     * 
     * @param dto Datos del pago a crear
     * @return Pago creado con vigencias calculadas
     * @throws ResourceNotFoundException si el usuario no existe
     * @throws InvalidPaymentException   si el pago viola reglas de negocio
     */
    public PagoDto create(PagoDto dto) {
        log.debug("Creando pago tipo {} para usuario {}", dto.getTipoPago(), dto.getUsuarioId());

        // Validar monto
        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("El monto debe ser mayor a cero", "MONTO_INVALIDO");
        }

        // Validar usuario
        UUID userId = UUID.fromString(dto.getUsuarioId());
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        if (!usuario.isActivo()) {
            throw new InvalidPaymentException("No se puede crear pago para usuario inactivo", "USUARIO_INACTIVO");
        }

        // Validar tipo de pago
        if (dto.getTipoPago() == null) {
            throw new InvalidPaymentException("Tipo de pago es requerido", "TIPO_PAGO_REQUERIDO");
        }

        Pago entity = PagoMapper.toEntity(dto, usuario);

        // Calcular vigencias según tipo de pago
        calcularVigencias(entity, dto);

        // Estado inicial
        if (entity.getEstadoPago() == null) {
            entity.setEstadoPago(EstadoPago.PENDIENTE);
        }

        Pago saved = pagoRepository.save(entity);

        // Si es paquete, crear registro en paquetes_pago
        if (saved.getTipoPago() == TipoPago.PAQUETE) {
            crearPaquetePago(saved, dto);
        }

        log.info("Pago creado: id={}, tipo={}, usuario={}", saved.getId(), saved.getTipoPago(), usuario.getDocumento());
        return PagoMapper.toDto(saved);
    }

    /**
     * Calcula vigencias según el tipo de pago.
     */
    private void calcularVigencias(Pago pago, PagoDto dto) {
        OffsetDateTime ahora = OffsetDateTime.now(timezone);

        switch (pago.getTipoPago()) {
            case DIARIO -> {
                // Vigencia solo para hoy
                OffsetDateTime inicioDia = ahora.toLocalDate().atStartOfDay(timezone).toOffsetDateTime();
                OffsetDateTime finDia = inicioDia.plusDays(1).minusNanos(1);
                pago.setVigenteDesde(inicioDia);
                pago.setVigenteHasta(finDia);
                log.debug("Pago DIARIO: vigencia {} - {}", inicioDia, finDia);
            }
            case MENSUAL -> {
                // Primer y último día del mes
                OffsetDateTime primerDia = ahora.with(TemporalAdjusters.firstDayOfMonth())
                        .toLocalDate().atStartOfDay(timezone).toOffsetDateTime();
                OffsetDateTime ultimoDia = ahora.with(TemporalAdjusters.lastDayOfMonth())
                        .toLocalDate().atTime(23, 59, 59, 999999999)
                        .atZone(timezone).toOffsetDateTime();
                pago.setVigenteDesde(primerDia);
                pago.setVigenteHasta(ultimoDia);
                log.debug("Pago MENSUAL: vigencia {} - {}", primerDia, ultimoDia);
            }
            case PAQUETE -> {
                // Para paquetes, las vigencias se manejan en DerechoUso
                // Aquí solo validamos que se hayan especificado días
                if (dto.getDiasPaquete() == null || dto.getDiasPaquete() <= 0) {
                    throw new InvalidPaymentException(
                            "Para pagos tipo PAQUETE debe especificar días > 0",
                            "DIAS_PAQUETE_REQUERIDOS");
                }
                pago.setVigenteDesde(null);
                pago.setVigenteHasta(null);
                log.debug("Pago PAQUETE: {} días", dto.getDiasPaquete());
            }
        }
    }

    /**
     * Crea registro de paquete de pago.
     */
    private void crearPaquetePago(Pago pago, PagoDto dto) {
        PaquetePago paquete = new PaquetePago();
        paquete.setPago(pago);
        paquete.setDias(dto.getDiasPaquete());
        paquete.setDiasRestantes(dto.getDiasPaquete());
        paquetePagoRepository.save(paquete);
        log.debug("PaquetePago creado: id={}, días={}", paquete.getId(), paquete.getDias());
    }

    /**
     * Actualiza un pago existente.
     * Solo permite actualizar campos específicos, no el tipo de pago ni vigencias.
     */
    public PagoDto update(UUID id, PagoDto dto) {
        log.debug("Actualizando pago {}", id);

        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));

        // Solo permitir actualizar estos campos
        if (dto.getMetodoPago() != null) {
            pago.setMetodoPago(dto.getMetodoPago());
        }
        if (dto.getEstadoPago() != null) {
            pago.setEstadoPago(dto.getEstadoPago());
        }
        if (dto.getReferenciaExterna() != null) {
            pago.setReferenciaExterna(dto.getReferenciaExterna());
        }
        if (dto.getCajero() != null) {
            pago.setCajero(dto.getCajero());
        }
        if (dto.getMetadatos() != null) {
            pago.setMetadatos(dto.getMetadatos());
        }

        Pago saved = pagoRepository.save(pago);
        log.info("Pago actualizado: id={}, estadoPago={}", saved.getId(), saved.getEstadoPago());
        return PagoMapper.toDto(saved);
    }

    /**
     * Obtiene un pago por ID.
     */
    @Transactional(readOnly = true)
    public PagoDto get(UUID id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        return PagoMapper.toDto(pago);
    }

    /**
     * Lista todos los pagos.
     */
    @Transactional(readOnly = true)
    public List<PagoDto> list() {
        return pagoRepository.findAll().stream()
                .map(PagoMapper::toDto)
                .toList();
    }

    /**
     * Lista pagos por usuario.
     */
    @Transactional(readOnly = true)
    public List<PagoDto> listByUsuario(UUID usuarioId) {
        return pagoRepository.findByUsuarioId(usuarioId).stream()
                .map(PagoMapper::toDto)
                .toList();
    }

    /**
     * Lista pagos por tipo.
     */
    @Transactional(readOnly = true)
    public List<PagoDto> listByTipo(TipoPago tipo) {
        return pagoRepository.findByTipoPago(tipo).stream()
                .map(PagoMapper::toDto)
                .toList();
    }

    /**
     * Lista pagos por estado.
     */
    @Transactional(readOnly = true)
    public List<PagoDto> listByEstado(EstadoPago estado) {
        return pagoRepository.findByEstadoPago(estado).stream()
                .map(PagoMapper::toDto)
                .toList();
    }

    /**
     * Lista pagos en rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<PagoDto> listByFechaRango(OffsetDateTime desde, OffsetDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new InvalidPaymentException("Fecha 'desde' debe ser anterior a 'hasta'", "RANGO_FECHAS_INVALIDO");
        }
        return pagoRepository.findByCreadoEnBetween(desde, hasta).stream()
                .map(PagoMapper::toDto)
                .toList();
    }

    /**
     * Aprueba un pago y genera automáticamente el derecho de uso correspondiente.
     * 
     * <p>
     * Lógica de generación de derechos:
     * <ul>
     * <li>DIARIO: DerechoUso válido solo para el día actual</li>
     * <li>MENSUAL: DerechoUso válido del primer al último día del mes</li>
     * <li>PAQUETE: DerechoUso válido 24h, consume 1 día del paquete</li>
     * </ul>
     * </p>
     * 
     * @param id ID del pago a aprobar
     * @return Pago aprobado con derecho de uso generado
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidPaymentException   si el pago ya está aprobado o rechazado
     * @since FASE 3.2
     */
    public PagoDto aprobar(UUID id) {
        log.debug("Aprobando pago {}", id);

        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));

        // Validar estado actual
        if (pago.getEstadoPago() == EstadoPago.APROBADO) {
            throw new InvalidPaymentException(
                    "El pago ya fue aprobado anteriormente",
                    "PAGO_YA_APROBADO");
        }
        if (pago.getEstadoPago() == EstadoPago.RECHAZADO) {
            throw new InvalidPaymentException(
                    "No se puede aprobar un pago previamente rechazado",
                    "PAGO_PREVIAMENTE_RECHAZADO");
        }

        // Cambiar estado a APROBADO
        pago.setEstadoPago(EstadoPago.APROBADO);
        Pago saved = pagoRepository.save(pago);

        // Generar derecho de uso automáticamente
        try {
            derechoUsoService.generarDerecho(saved.getId());
            log.info("Pago aprobado y derecho de uso generado: id={}, usuario={}, tipo={}",
                    saved.getId(), saved.getUsuario().getDocumento(), saved.getTipoPago());
        } catch (Exception e) {
            log.error("Error al generar derecho de uso para pago {}: {}", saved.getId(), e.getMessage(), e);
            // Revertir aprobación si falla la generación del derecho
            pago.setEstadoPago(EstadoPago.PENDIENTE);
            pagoRepository.save(pago);
            throw new InvalidPaymentException(
                    "Error al generar derecho de uso: " + e.getMessage(),
                    "ERROR_GENERAR_DERECHO");
        }

        return PagoMapper.toDto(saved);
    }

    /**
     * Rechaza un pago.
     * 
     * @param id ID del pago a rechazar
     * @return Pago rechazado
     * @throws ResourceNotFoundException si el pago no existe
     * @throws InvalidPaymentException   si el pago ya está aprobado o rechazado
     * @since FASE 3.2
     */
    public PagoDto rechazar(UUID id) {
        log.debug("Rechazando pago {}", id);

        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));

        // Validar estado actual
        if (pago.getEstadoPago() == EstadoPago.APROBADO) {
            throw new InvalidPaymentException(
                    "No se puede rechazar un pago ya aprobado",
                    "PAGO_YA_APROBADO");
        }
        if (pago.getEstadoPago() == EstadoPago.RECHAZADO) {
            throw new InvalidPaymentException(
                    "El pago ya fue rechazado anteriormente",
                    "PAGO_YA_RECHAZADO");
        }

        // Cambiar estado a RECHAZADO
        pago.setEstadoPago(EstadoPago.RECHAZADO);
        Pago saved = pagoRepository.save(pago);

        log.info("Pago rechazado: id={}, usuario={}", saved.getId(), saved.getUsuario().getDocumento());
        return PagoMapper.toDto(saved);
    }

    /**
     * Revierte un pago previamente aprobado y cataloga el registro como devolución.
     *
     * <p>
     * Reglas:
     * <ul>
     * <li>Solo se pueden revertir pagos en estado APROBADO.</li>
     * <li>El estado final del pago queda como RECHAZADO (hasta que exista un estado
     * REVERTIDO).</li>
     * <li>Se desactivan los derechos de uso originados por el pago.</li>
     * <li>Si el pago es de tipo PAQUETE y se consumió un día, se intenta restaurar
     * uno.</li>
     * </ul>
     * </p>
     *
     * @param id ID del pago a revertir
     * @return Pago actualizado
     */
    public PagoDto revertir(UUID id) {
        log.debug("Revirtiendo pago {} (devolución)", id);

        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));

        if (pago.getEstadoPago() != EstadoPago.APROBADO) {
            throw new InvalidPaymentException(
                    "Solo se pueden revertir pagos APROBADOS. Estado actual: " + pago.getEstadoPago(),
                    "PAGO_NO_APROBADO_PARA_REVERSION");
        }

        // Desactivar derechos de uso asociados a este pago
        try {
            var derechos = derechoUsoRepository.findByPagoOrigenId(id);
            for (var d : derechos) {
                if (d.isActivo()) {
                    d.setActivo(false);
                }
            }
            if (!derechos.isEmpty()) {
                derechoUsoRepository.saveAll(derechos);
            }
        } catch (Exception e) {
            log.warn("No fue posible desactivar derechos de uso para el pago {}: {}", id, e.getMessage());
        }

        // Restaurar día de paquete si aplica
        try {
            if (pago.getTipoPago() == co.cellano.edufeed.backend.model.enums.TipoPago.PAQUETE) {
                paquetePagoService.restaurarDia(id);
            }
        } catch (Exception e) {
            log.warn("No fue posible restaurar día de paquete para el pago {}: {}", id, e.getMessage());
        }

        // Marcar como RECHAZADO para efectos de catálogo (devolución)
        pago.setEstadoPago(EstadoPago.RECHAZADO);
        Pago saved = pagoRepository.save(pago);

        log.info("Pago revertido (devolución): id={}, usuario={}", saved.getId(), saved.getUsuario().getDocumento());
        return PagoMapper.toDto(saved);
    }
}
