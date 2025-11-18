package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.service.PagoService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasAnyRole('ADMIN','OPERADOR_CAJA')")
public class PagoController {
    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoDto> create(@Valid @RequestBody PagoDto dto) {
        PagoDto created = pagoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagoDto> update(@PathVariable("id") UUID id, @Valid @RequestBody PagoDto dto) {
        PagoDto updated = pagoService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoDto> get(@PathVariable("id") UUID id) {
        PagoDto pago = pagoService.get(id);
        return ResponseEntity.ok(pago);
    }

    @GetMapping
    public List<PagoDto> list() {
        return pagoService.list();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<PagoDto> listByUsuario(@PathVariable("usuarioId") UUID usuarioId) {
        return pagoService.listByUsuario(usuarioId);
    }

    @GetMapping("/tipo/{tipo}")
    public List<PagoDto> listByTipo(@PathVariable("tipo") TipoPago tipo) {
        return pagoService.listByTipo(tipo);
    }

    @GetMapping("/estado/{estado}")
    public List<PagoDto> listByEstado(@PathVariable("estado") EstadoPago estado) {
        return pagoService.listByEstado(estado);
    }

    @GetMapping("/rango")
    public List<PagoDto> listByFechaRango(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return pagoService.listByFechaRango(desde, hasta);
    }

    // DIARIO: válido solo hoy | MENSUAL: mes completo | PAQUETE: 24h y consume 1
    // día
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<PagoDto> aprobar(@PathVariable("id") UUID id) {
        PagoDto aprobado = pagoService.aprobar(id);
        return ResponseEntity.ok(aprobado);
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<PagoDto> rechazar(@PathVariable("id") UUID id) {
        PagoDto rechazado = pagoService.rechazar(id);
        return ResponseEntity.ok(rechazado);
    }

    @PutMapping("/{id}/revertir")
    public ResponseEntity<PagoDto> revertir(@PathVariable("id") UUID id) {
        PagoDto revertido = pagoService.revertir(id);
        return ResponseEntity.ok(revertido);
    }
}
