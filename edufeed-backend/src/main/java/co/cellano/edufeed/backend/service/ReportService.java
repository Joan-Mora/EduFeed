package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.response.AsistenciasDiariasItem;
import co.cellano.edufeed.backend.dto.response.DerechoActivoItem;
import co.cellano.edufeed.backend.dto.response.IngresosDiariosItem;
import co.cellano.edufeed.backend.dto.response.RechazosDiariosItem;
import co.cellano.edufeed.backend.repository.AccesoRepository;
import co.cellano.edufeed.backend.repository.DerechoUsoRepository;
import co.cellano.edufeed.backend.repository.PagoRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Servicio para generación de reportes administrativos.
 * 
 * @since FASE 2.2, extendido en FASE 3.3
 */
@Service
public class ReportService {

    private final PagoRepository pagoRepository;
    private final AccesoRepository accesoRepository;
    private final DerechoUsoRepository derechoUsoRepository;
    private final ZoneId timezone;

    public ReportService(PagoRepository pagoRepository,
            AccesoRepository accesoRepository,
            DerechoUsoRepository derechoUsoRepository) {
        this.pagoRepository = pagoRepository;
        this.accesoRepository = accesoRepository;
        this.derechoUsoRepository = derechoUsoRepository;
        this.timezone = ZoneId.of("America/Bogota");
    }

    /**
     * Obtiene ingresos diarios agregados a partir de la tabla pagos.
     */
    public List<IngresosDiariosItem> ingresosDiarios(OffsetDateTime desde, OffsetDateTime hasta) {
        List<Object[]> rows = pagoRepository.aggregateIngresosDiarios(desde, hasta);
        List<IngresosDiariosItem> out = new ArrayList<>();
        for (Object[] r : rows) {
            IngresosDiariosItem item = new IngresosDiariosItem();
            item.setDia(((java.sql.Date) r[0]).toLocalDate());
            item.setTipoPago((String) r[1]);
            item.setMetodoPago((String) r[2]);
            item.setCantidad(((Number) r[3]).longValue());
            item.setTotal((BigDecimal) r[4]);
            out.add(item);
        }
        return out;
    }

    /**
     * Devuelve un resumen (suma total) de ingresos para el periodo.
     */
    public BigDecimal resumenIngresos(OffsetDateTime desde, OffsetDateTime hasta) {
        return ingresosDiarios(desde, hasta).stream()
                .map(IngresosDiariosItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene asistencias diarias (accesos exitosos) en el periodo especificado.
     * 
     * @since FASE 3.3
     */
    public List<AsistenciasDiariasItem> asistenciasDiarias(OffsetDateTime desde, OffsetDateTime hasta) {
        List<Object[]> rows = accesoRepository.aggregateAsistenciasDiarias(desde, hasta);
        List<AsistenciasDiariasItem> out = new ArrayList<>();
        for (Object[] r : rows) {
            AsistenciasDiariasItem item = new AsistenciasDiariasItem();
            item.setDia(((java.sql.Date) r[0]).toLocalDate());
            item.setTotalAccesos(((Number) r[1]).longValue());
            item.setUsuariosUnicos(((Number) r[2]).longValue());
            out.add(item);
        }
        return out;
    }

    /**
     * Obtiene rechazos diarios (accesos fallidos) por motivo en el periodo especificado.
     * 
     * @since FASE 3.3
     */
    public List<RechazosDiariosItem> rechazosDiarios(OffsetDateTime desde, OffsetDateTime hasta) {
        List<Object[]> rows = accesoRepository.aggregateRechazosDiarios(desde, hasta);
        List<RechazosDiariosItem> out = new ArrayList<>();
        for (Object[] r : rows) {
            RechazosDiariosItem item = new RechazosDiariosItem();
            item.setDia(((java.sql.Date) r[0]).toLocalDate());
            item.setMotivoRechazo((String) r[1]);
            item.setCantidad(((Number) r[2]).longValue());
            out.add(item);
        }
        return out;
    }

    /**
     * Obtiene derechos de uso activos con información detallada de usuarios y paquetes.
     * 
     * @since FASE 3.3
     */
    public List<DerechoActivoItem> derechosActivos() {
        OffsetDateTime ahora = OffsetDateTime.now(timezone);
        List<Object[]> rows = derechoUsoRepository.findDerechosActivosConDetalle(ahora);
        List<DerechoActivoItem> out = new ArrayList<>();
        for (Object[] r : rows) {
            DerechoActivoItem item = new DerechoActivoItem();
            item.setUsuarioDocumento((String) r[0]);
            item.setUsuarioNombre((String) r[1]);
            item.setTipoDerecho((String) r[2]);
            
            // Convertir Timestamp a OffsetDateTime
            if (r[3] instanceof Timestamp) {
                item.setVigenteDesde(((Timestamp) r[3]).toInstant().atZone(timezone).toOffsetDateTime());
            }
            if (r[4] instanceof Timestamp) {
                item.setVigenteHasta(((Timestamp) r[4]).toInstant().atZone(timezone).toOffsetDateTime());
            }
            
            // dias_restantes puede ser null si no es paquete
            if (r[5] != null) {
                item.setDiasRestantes(((Number) r[5]).intValue());
            }
            out.add(item);
        }
        return out;
    }
}
