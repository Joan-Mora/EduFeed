package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.AccesoDto;
import co.cellano.edufeed.backend.dto.request.AccesoCheckRequest;
import co.cellano.edufeed.backend.dto.response.AccesoCheckResponse;
import co.cellano.edufeed.backend.dto.response.OrientacionCajaResponse;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.mapper.AccesoMapper;
import co.cellano.edufeed.backend.mapper.DerechoUsoMapper;
import co.cellano.edufeed.backend.mapper.UsuarioMapper;
import co.cellano.edufeed.backend.model.Acceso;
import co.cellano.edufeed.backend.model.DerechoUso;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.EstadoAcceso;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.repository.AccesoRepository;
import co.cellano.edufeed.backend.repository.DerechoUsoRepository;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para control de acceso y verificación de derechos de uso.
 * 
 * <p>
 * Implementa los requisitos funcionales:
 * <ul>
 * <li>RF-03: Control de derecho adquirido</li>
 * <li>RF-04: Orientación a caja</li>
 * <li>RF-09: Historial de accesos</li>
 * </ul>
 * </p>
 * 
 * @since FASE 2.3
 */
@Service
@Transactional
public class AccesoService {
    private static final Logger log = LoggerFactory.getLogger(AccesoService.class);
    private static final ZoneId TIMEZONE = ZoneId.of("America/Bogota");

    // Configuración de caja (en producción esto vendría de configuración externa)
    private static final String UBICACION_CAJA = "Planta baja, entrada principal, lado derecho";
    private static final String HORARIO_ATENCION = "Lunes a Viernes: 7:00 AM - 5:00 PM";

    private final AccesoRepository accesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DerechoUsoRepository derechoUsoRepository;
    private final PaquetePagoService paquetePagoService;

    public AccesoService(
            AccesoRepository accesoRepository,
            UsuarioRepository usuarioRepository,
            DerechoUsoRepository derechoUsoRepository,
            PaquetePagoService paquetePagoService) {
        this.accesoRepository = accesoRepository;
        this.usuarioRepository = usuarioRepository;
        this.derechoUsoRepository = derechoUsoRepository;
        this.paquetePagoService = paquetePagoService;
    }

    /**
     * Verifica el derecho de acceso de un usuario y registra el intento.
     * 
     * @param request Solicitud con usuarioId y modalidad biométrica
     * @return Respuesta con resultado de verificación
     */
    public AccesoCheckResponse verificarAcceso(AccesoCheckRequest request) {
        log.debug("Verificando acceso para usuario: {}, modalidad: {}",
                request.getUsuarioId(), request.getModalidad());

        // 1. Validar que usuario existe
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.getUsuarioId()));

        // 2. Verificar que usuario está activo
        if (!usuario.isActivo()) {
            log.info("Acceso denegado: usuario inactivo - {}", usuario.getDocumento());
            return denegarAcceso(usuario, "USUARIO_INACTIVO", request.getModalidad());
        }

        // 3. Buscar derecho vigente
        OffsetDateTime ahora = OffsetDateTime.now(TIMEZONE);
        Optional<DerechoUso> derechoOpt = derechoUsoRepository.findDerechoVigente(
                request.getUsuarioId(), ahora);

        if (derechoOpt.isEmpty()) {
            log.info("Acceso denegado: sin derecho vigente - {}", usuario.getDocumento());
            return denegarAcceso(usuario, "SIN_DERECHO_VIGENTE", request.getModalidad());
        }

        DerechoUso derecho = derechoOpt.get();

        // 4. Si es paquete, verificar y consumir día
        if (derecho.getTipoDerecho() == TipoPago.PAQUETE) {
            UUID pagoId = derecho.getPagoOrigen().getId();

            if (!paquetePagoService.tieneDiasDisponibles(pagoId)) {
                log.info("Acceso denegado: paquete agotado - usuario: {}, pago: {}",
                        usuario.getDocumento(), pagoId);
                return denegarAcceso(usuario, "PAQUETE_AGOTADO", request.getModalidad());
            }

            // Consumir día del paquete
            paquetePagoService.consumirDia(pagoId);
            log.debug("Día consumido del paquete: pago={}, días restantes={}",
                    pagoId, paquetePagoService.obtenerDiasRestantes(pagoId));
        }

        // 5. Registrar acceso aprobado
        Acceso acceso = new Acceso();
        acceso.setUsuario(usuario);
        acceso.setDerecho(derecho);
        acceso.setEstado(EstadoAcceso.APROBADO);
        acceso.setModalidad(request.getModalidad());
        acceso.setFechaHora(ahora);
        accesoRepository.save(acceso);

        log.info("Acceso APROBADO: usuario={}, documento={}, modalidad={}, derecho={}",
                usuario.getId(), usuario.getDocumento(), request.getModalidad(), derecho.getTipoDerecho());

        // 6. Retornar respuesta positiva
        return AccesoCheckResponse.builder()
                .permitido(true)
                .usuario(UsuarioMapper.toDto(usuario))
                .derecho(DerechoUsoMapper.toDto(derecho))
                .modalidad(request.getModalidad())
                .timestamp(ahora)
                .build();
    }

    /**
     * Registra un acceso denegado y genera orientación a caja.
     * 
     * @param usuario   Usuario al que se deniega acceso
     * @param motivo    Motivo de la denegación
     * @param modalidad Modalidad biométrica utilizada
     * @return Respuesta con motivo e instrucciones de orientación
     */
    private AccesoCheckResponse denegarAcceso(Usuario usuario, String motivo, Modalidad modalidad) {
        OffsetDateTime ahora = OffsetDateTime.now(TIMEZONE);

        // Registrar acceso denegado
        Acceso acceso = new Acceso();
        acceso.setUsuario(usuario);
        acceso.setEstado(EstadoAcceso.DENEGADO);
        acceso.setModalidad(modalidad);
        acceso.setMotivo(motivo);
        acceso.setFechaHora(ahora);
        accesoRepository.save(acceso);

        log.info("Acceso DENEGADO: usuario={}, documento={}, motivo={}",
                usuario.getId(), usuario.getDocumento(), motivo);

        // Generar orientación a caja
        OrientacionCajaResponse orientacion = generarOrientacionCaja(usuario, motivo);

        // Retornar respuesta negativa
        return AccesoCheckResponse.builder()
                .permitido(false)
                .usuario(UsuarioMapper.toDto(usuario))
                .motivo(motivo)
                .modalidad(modalidad)
                .orientacionCaja(orientacion)
                .timestamp(ahora)
                .build();
    }

    /**
     * Genera las instrucciones de orientación a caja según el motivo de denegación.
     * Implementa el requisito funcional RF-04.
     * 
     * @param usuario Usuario al que se le deniega acceso
     * @param motivo  Motivo de la denegación
     * @return Información de orientación a caja
     */
    private OrientacionCajaResponse generarOrientacionCaja(Usuario usuario, String motivo) {
        String mensaje;

        switch (motivo) {
            case "SIN_DERECHO_VIGENTE":
                mensaje = "No tiene un pago activo. Por favor diríjase a caja para realizar el pago.";
                break;
            case "PAQUETE_AGOTADO":
                mensaje = "Su paquete de días se ha agotado. Por favor diríjase a caja para renovar.";
                break;
            case "USUARIO_INACTIVO":
                mensaje = "Su usuario está inactivo. Por favor diríjase a administración.";
                break;
            default:
                mensaje = "Por favor diríjase a caja para más información.";
        }

        return OrientacionCajaResponse.builder()
                .mensaje(mensaje)
                .ubicacionCaja(UBICACION_CAJA)
                .horarioAtencion(HORARIO_ATENCION)
                .referencia(usuario.getDocumento())
                .codigoQR(generarCodigoQR(usuario))
                .build();
    }

    /**
     * Genera un código QR para identificación del usuario en caja.
     * 
     * @param usuario Usuario
     * @return String con información codificable en QR
     */
    private String generarCodigoQR(Usuario usuario) {
        // Formato: EDUFEED:USER:{documento}:TIMESTAMP:{timestamp}
        return String.format("EDUFEED:USER:%s:TIMESTAMP:%d",
                usuario.getDocumento(),
                System.currentTimeMillis());
    }

    /**
     * Obtiene el historial de accesos con filtros opcionales.
     * Implementa el requisito funcional RF-09.
     * 
     * @param usuarioId ID del usuario (opcional)
     * @param inicio    Fecha/hora de inicio (opcional)
     * @param fin       Fecha/hora de fin (opcional)
     * @param estado    Estado del acceso (opcional)
     * @param pageable  Información de paginación
     * @return Página con accesos que cumplen los filtros
     */
    @Transactional(readOnly = true)
    public Page<AccesoDto> obtenerHistorial(
            UUID usuarioId,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            EstadoAcceso estado,
            Pageable pageable) {

        log.debug("Consultando historial de accesos: usuario={}, inicio={}, fin={}, estado={}",
                usuarioId, inicio, fin, estado);

        // Construir query dinámica con Specification
        Specification<Acceso> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            if (inicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creadoEn"), inicio));
            }

            if (fin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creadoEn"), fin));
            }

            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Acceso> accesos = accesoRepository.findAll(spec, pageable);

        log.debug("Historial consultado: {} resultados de {}",
                accesos.getNumberOfElements(), accesos.getTotalElements());

        return accesos.map(AccesoMapper::toDto);
    }

    /**
     * Obtiene los accesos de un usuario en un día específico.
     * 
     * @param usuarioId ID del usuario
     * @param fecha     Fecha a consultar
     * @return Lista de accesos del usuario en esa fecha
     */
    @Transactional(readOnly = true)
    public List<AccesoDto> obtenerAccesosPorDia(UUID usuarioId, OffsetDateTime fecha) {
        OffsetDateTime inicioDia = fecha.toLocalDate().atStartOfDay(TIMEZONE).toOffsetDateTime();
        OffsetDateTime finDia = inicioDia.plusDays(1).minusNanos(1);

        log.debug("Consultando accesos del día: usuario={}, fecha={}", usuarioId, fecha.toLocalDate());

        Specification<Acceso> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("usuario").get("id"), usuarioId),
                cb.between(root.get("creadoEn"), inicioDia, finDia));

        List<Acceso> accesos = accesoRepository.findAll(spec);
        return accesos.stream()
                .map(AccesoMapper::toDto)
                .toList();
    }
}