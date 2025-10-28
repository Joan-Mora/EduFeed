package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.request.ConciliarTransaccionRequest;
import co.cellano.edufeed.backend.dto.request.WebhookPagoRequest;
import co.cellano.edufeed.backend.dto.response.TransaccionCajaResponse;
import co.cellano.edufeed.backend.service.TransaccionCajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Integración de Caja", description = "Webhooks, consultas y conciliación de transacciones POS/caja")
public class WebhookController {

    private final TransaccionCajaService transaccionCajaService;

    public WebhookController(TransaccionCajaService transaccionCajaService) {
        this.transaccionCajaService = transaccionCajaService;
    }

    // ============ WEBHOOK (recepción de transacciones) ============

    @PostMapping("/webhooks/pagos")
    @Operation(
        summary = "Webhook de pagos",
        description = "Recibe notificaciones de pagos desde caja/POS y almacena la transacción. Intenta conciliación automática por referencia externa."
    )
    public ResponseEntity<Map<String, Object>> recibirPago(
            @Valid @RequestBody WebhookPagoRequest request
    ) {
        Map<String, Object> res = transaccionCajaService.procesarWebhook(request);
        return ResponseEntity.ok(res);
    }

    // ============ CONSULTAS DE TRANSACCIONES ============

    @GetMapping("/transacciones")
    @Operation(
        summary = "Listar transacciones",
        description = "Obtiene todas las transacciones con paginación."
    )
    public ResponseEntity<Page<TransaccionCajaResponse>> listarTransacciones(
            @Parameter(description = "Número de página (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TransaccionCajaResponse> transacciones = transaccionCajaService.listarTransacciones(page, size);
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/transacciones/{id}")
    @Operation(
        summary = "Obtener transacción por ID",
        description = "Obtiene los detalles de una transacción específica."
    )
    public ResponseEntity<TransaccionCajaResponse> obtenerTransaccion(
            @Parameter(description = "ID de la transacción")
        @PathVariable("id") UUID id
    ) {
        TransaccionCajaResponse transaccion = transaccionCajaService.obtenerPorId(id);
        return ResponseEntity.ok(transaccion);
    }

    @GetMapping("/transacciones/no-conciliadas")
    @Operation(
        summary = "Listar transacciones no conciliadas",
        description = "Obtiene todas las transacciones que aún no han sido conciliadas con un pago."
    )
    public ResponseEntity<List<TransaccionCajaResponse>> listarNoConciliadas() {
        List<TransaccionCajaResponse> transacciones = transaccionCajaService.listarNoConciliadas();
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/transacciones/conciliadas")
    @Operation(
        summary = "Listar transacciones conciliadas",
        description = "Obtiene todas las transacciones que ya han sido conciliadas con un pago."
    )
    public ResponseEntity<List<TransaccionCajaResponse>> listarConciliadas() {
        List<TransaccionCajaResponse> transacciones = transaccionCajaService.listarConciliadas();
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/transacciones/proveedor/{proveedor}")
    @Operation(
        summary = "Listar transacciones por proveedor",
        description = "Obtiene todas las transacciones de un proveedor específico (ej: 'WOMPI', 'MERCADOPAGO')."
    )
    public ResponseEntity<List<TransaccionCajaResponse>> listarPorProveedor(
            @Parameter(description = "Nombre del proveedor de pagos")
        @PathVariable("proveedor") String proveedor
    ) {
        List<TransaccionCajaResponse> transacciones = transaccionCajaService.listarPorProveedor(proveedor);
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/transacciones/estado/{estado}")
    @Operation(
        summary = "Listar transacciones por estado",
        description = "Obtiene transacciones filtradas por estado (PENDIENTE, APROBADO, RECHAZADO, ANULADO)."
    )
    public ResponseEntity<List<TransaccionCajaResponse>> listarPorEstado(
            @Parameter(description = "Estado de la transacción")
        @PathVariable("estado") String estado
    ) {
        List<TransaccionCajaResponse> transacciones = transaccionCajaService.listarPorEstado(estado);
        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/transacciones/rango-fechas")
    @Operation(
        summary = "Listar transacciones por rango de fechas",
        description = "Obtiene transacciones recibidas en un rango de fechas específico."
    )
    public ResponseEntity<List<TransaccionCajaResponse>> listarPorRangoFechas(
            @Parameter(description = "Fecha de inicio (ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @Parameter(description = "Fecha de fin (ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta
    ) {
        List<TransaccionCajaResponse> transacciones = transaccionCajaService.listarPorRangoFechas(desde, hasta);
        return ResponseEntity.ok(transacciones);
    }

    // ============ CONCILIACIÓN ============

    @PutMapping("/transacciones/{id}/conciliar")
    @Operation(
        summary = "Conciliar transacción manualmente",
        description = "Concilia manualmente una transacción con un pago específico."
    )
    public ResponseEntity<TransaccionCajaResponse> conciliarManual(
            @Parameter(description = "ID de la transacción a conciliar")
        @PathVariable("id") UUID id,
            @Valid @RequestBody ConciliarTransaccionRequest request
    ) {
        TransaccionCajaResponse transaccion = transaccionCajaService.conciliarManual(id, request.getPagoId());
        return ResponseEntity.ok(transaccion);
    }

    @PutMapping("/transacciones/{id}/desconciliar")
    @Operation(
        summary = "Desconciliar transacción",
        description = "Revierte la conciliación de una transacción (útil si se concilió incorrectamente)."
    )
    public ResponseEntity<TransaccionCajaResponse> desconciliar(
            @Parameter(description = "ID de la transacción a desconciliar")
        @PathVariable("id") UUID id
    ) {
        TransaccionCajaResponse transaccion = transaccionCajaService.desconciliar(id);
        return ResponseEntity.ok(transaccion);
    }

    // ============ ESTADÍSTICAS Y REPORTES ============

    @GetMapping("/transacciones/estadisticas")
    @Operation(
        summary = "Estadísticas de conciliación",
        description = "Obtiene estadísticas generales de transacciones (total, conciliadas, no conciliadas, porcentaje)."
    )
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        Map<String, Object> stats = transaccionCajaService.obtenerEstadisticas();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/transacciones/contar/no-conciliadas")
    @Operation(
        summary = "Contar transacciones no conciliadas",
        description = "Retorna la cantidad de transacciones pendientes de conciliación."
    )
    public ResponseEntity<Map<String, Long>> contarNoConciliadas() {
        long count = transaccionCajaService.contarNoConciliadas();
        return ResponseEntity.ok(Map.of("noConciliadas", count));
    }
}
