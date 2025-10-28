package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.request.WebhookPagoRequest;
import co.cellano.edufeed.backend.dto.response.TransaccionCajaResponse;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.TransaccionCaja;
import co.cellano.edufeed.backend.repository.PagoRepository;
import co.cellano.edufeed.backend.repository.TransaccionCajaRepository;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransaccionCajaService {

    private final TransaccionCajaRepository transaccionCajaRepository;
    private final PagoRepository pagoRepository;

    public TransaccionCajaService(TransaccionCajaRepository transaccionCajaRepository, PagoRepository pagoRepository) {
        this.transaccionCajaRepository = transaccionCajaRepository;
        this.pagoRepository = pagoRepository;
    }

    /**
     * Procesa un webhook de pago: guarda la transacción y realiza un intento simple de conciliación
     * contra la tabla de pagos usando la referencia externa.
     */
    @Transactional
    public Map<String, Object> procesarWebhook(WebhookPagoRequest req) {
        TransaccionCaja tx = new TransaccionCaja();
        tx.setProveedor(req.getProveedor());
        tx.setReferenciaExterna(req.getReferenciaExterna());
        tx.setMonto(req.getMonto());
        tx.setMetodoPago(req.getMetodoPago());
        tx.setEstado(req.getEstado());
        tx.setPayload(req.getPayload());

        // Intento de conciliación automática por referencia externa
        if (req.getReferenciaExterna() != null && !req.getReferenciaExterna().isBlank()) {
            pagoRepository.findByReferenciaExterna(req.getReferenciaExterna())
                    .ifPresent(pago -> {
                        tx.setPago(pago);
                        tx.setConciliado(true);
                    });
        }

        TransaccionCaja saved = transaccionCajaRepository.save(tx);

        Map<String, Object> res = new HashMap<>();
        res.put("recibido", true);
        res.put("transaccionId", saved.getId());
        res.put("conciliado", saved.isConciliado());
        if (saved.getPago() != null) {
            res.put("pagoId", saved.getPago().getId());
        }
        return res;
    }

    /**
     * Obtiene todas las transacciones con paginación.
     */
    @Transactional(readOnly = true)
    public Page<TransaccionCajaResponse> listarTransacciones(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransaccionCaja> transacciones = transaccionCajaRepository.findAllByOrderByRecibidoEnDesc(pageable);
        return transacciones.map(this::toResponse);
    }

    /**
     * Obtiene transacciones no conciliadas.
     */
    @Transactional(readOnly = true)
    public List<TransaccionCajaResponse> listarNoConciliadas() {
        List<Object[]> rows = transaccionCajaRepository.findConDetallesByConciliado(false);
        return rows.stream().map(this::toResponseFromArray).collect(Collectors.toList());
    }

    /**
     * Obtiene transacciones conciliadas.
     */
    @Transactional(readOnly = true)
    public List<TransaccionCajaResponse> listarConciliadas() {
        List<Object[]> rows = transaccionCajaRepository.findConDetallesByConciliado(true);
        return rows.stream().map(this::toResponseFromArray).collect(Collectors.toList());
    }

    /**
     * Obtiene transacciones por proveedor.
     */
    @Transactional(readOnly = true)
    public List<TransaccionCajaResponse> listarPorProveedor(String proveedor) {
        return transaccionCajaRepository.findByProveedorOrderByRecibidoEnDesc(proveedor)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene transacciones por estado.
     */
    @Transactional(readOnly = true)
    public List<TransaccionCajaResponse> listarPorEstado(String estado) {
        return transaccionCajaRepository.findByEstadoOrderByRecibidoEnDesc(estado)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene transacciones por rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<TransaccionCajaResponse> listarPorRangoFechas(OffsetDateTime desde, OffsetDateTime hasta) {
        return transaccionCajaRepository.findByRangoFechas(desde, hasta)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una transacción por ID.
     */
    @Transactional(readOnly = true)
    public TransaccionCajaResponse obtenerPorId(UUID id) {
        TransaccionCaja tx = transaccionCajaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción", id));
        return toResponse(tx);
    }

    /**
     * Concilia manualmente una transacción con un pago.
     */
    @Transactional
    public TransaccionCajaResponse conciliarManual(UUID transaccionId, UUID pagoId) {
        TransaccionCaja tx = transaccionCajaRepository.findById(transaccionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción", transaccionId));
        
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", pagoId));

        tx.setPago(pago);
        tx.setConciliado(true);
        
        TransaccionCaja saved = transaccionCajaRepository.save(tx);
        return toResponse(saved);
    }

    /**
     * Desconcilia una transacción (por ejemplo, si se concilió incorrectamente).
     */
    @Transactional
    public TransaccionCajaResponse desconciliar(UUID transaccionId) {
        TransaccionCaja tx = transaccionCajaRepository.findById(transaccionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción", transaccionId));

        tx.setPago(null);
        tx.setConciliado(false);
        
        TransaccionCaja saved = transaccionCajaRepository.save(tx);
        return toResponse(saved);
    }

    /**
     * Cuenta transacciones no conciliadas.
     */
    @Transactional(readOnly = true)
    public long contarNoConciliadas() {
        return transaccionCajaRepository.countNoConciliadas();
    }

    /**
     * Obtiene estadísticas de transacciones.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas() {
        long total = transaccionCajaRepository.count();
        long noConciliadas = transaccionCajaRepository.countNoConciliadas();
        long conciliadas = total - noConciliadas;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("conciliadas", conciliadas);
        stats.put("noConciliadas", noConciliadas);
        stats.put("porcentajeConciliacion", total > 0 ? (conciliadas * 100.0 / total) : 0.0);

        return stats;
    }

    // Métodos auxiliares de conversión
    
    private TransaccionCajaResponse toResponse(TransaccionCaja tx) {
        TransaccionCajaResponse response = new TransaccionCajaResponse();
        response.setId(tx.getId());
        response.setProveedor(tx.getProveedor());
        response.setReferenciaExterna(tx.getReferenciaExterna());
        response.setMonto(tx.getMonto());
        response.setMetodoPago(tx.getMetodoPago());
        response.setEstado(tx.getEstado());
        response.setRecibidoEn(tx.getRecibidoEn());
        response.setConciliado(tx.isConciliado());
        
        if (tx.getPago() != null) {
            response.setPagoId(tx.getPago().getId());
            if (tx.getPago().getUsuario() != null) {
                response.setUsuarioDocumento(tx.getPago().getUsuario().getDocumento());
                response.setUsuarioNombre(tx.getPago().getUsuario().getNombreCompleto());
            }
        }
        
        return response;
    }

    private TransaccionCajaResponse toResponseFromArray(Object[] row) {
        // Mapeo desde query JPQL: id, proveedor, referenciaExterna, monto, metodoPago,
        // estado, recibidoEn, conciliado, pagoId, usuarioDocumento, usuarioNombre
        return new TransaccionCajaResponse(
                (UUID) row[0],        // id
                (String) row[1],      // proveedor
                (String) row[2],      // referenciaExterna
                (java.math.BigDecimal) row[3], // monto
                (String) row[4],      // metodoPago
                (String) row[5],      // estado
                (OffsetDateTime) row[6], // recibidoEn
                (Boolean) row[7],     // conciliado
                (UUID) row[8],        // pagoId
                (String) row[9],      // usuarioDocumento
                (String) row[10]      // usuarioNombre
        );
    }
}
