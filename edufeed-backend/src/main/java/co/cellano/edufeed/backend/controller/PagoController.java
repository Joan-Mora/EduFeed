package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para gestión de pagos.
 * 
 * @since FASE 1, extendido en FASE 2.2
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Crea un nuevo pago con validaciones y cálculo automático de vigencias.
     * 
     * POST /api/pagos
     */
    @PostMapping
    public ResponseEntity<PagoDto> create(@Valid @RequestBody PagoDto dto) {
        PagoDto created = pagoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza un pago existente.
     * Solo permite actualizar campos específicos (método, estado, referencia,
     * cajero, metadatos).
     * 
     * PUT /api/pagos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PagoDto> update(@PathVariable UUID id, @Valid @RequestBody PagoDto dto) {
        PagoDto updated = pagoService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Obtiene un pago por ID.
     * 
     * GET /api/pagos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PagoDto> get(@PathVariable UUID id) {
        PagoDto pago = pagoService.get(id);
        return ResponseEntity.ok(pago);
    }

    /**
     * Lista todos los pagos.
     * 
     * GET /api/pagos
     */
    @GetMapping
    public List<PagoDto> list() {
        return pagoService.list();
    }

    /**
     * Lista pagos por usuario.
     * 
     * GET /api/pagos/usuario/{usuarioId}
     */
    @GetMapping("/usuario/{usuarioId}")
    public List<PagoDto> listByUsuario(@PathVariable UUID usuarioId) {
        return pagoService.listByUsuario(usuarioId);
    }

    /**
     * Lista pagos por tipo.
     * 
     * GET /api/pagos/tipo/{tipo}
     */
    @GetMapping("/tipo/{tipo}")
    public List<PagoDto> listByTipo(@PathVariable TipoPago tipo) {
        return pagoService.listByTipo(tipo);
    }

    /**
     * Lista pagos por estado.
     * 
     * GET /api/pagos/estado/{estado}
     */
    @GetMapping("/estado/{estado}")
    public List<PagoDto> listByEstado(@PathVariable EstadoPago estado) {
        return pagoService.listByEstado(estado);
    }

    /**
     * Lista pagos en rango de fechas.
     * 
     * GET
     * /api/pagos/rango?desde=2025-01-01T00:00:00-05:00&hasta=2025-01-31T23:59:59-05:00
     */
    @GetMapping("/rango")
    public List<PagoDto> listByFechaRango(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return pagoService.listByFechaRango(desde, hasta);
    }
}
