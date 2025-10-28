package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.response.AsistenciasDiariasItem;
import co.cellano.edufeed.backend.dto.response.DerechoActivoItem;
import co.cellano.edufeed.backend.dto.response.IngresosDiariosItem;
import co.cellano.edufeed.backend.dto.response.RechazosDiariosItem;
import co.cellano.edufeed.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para reportes administrativos.
 * 
 * @since FASE 2.2, extendido en FASE 3.3
 */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
@Tag(name = "Reportes", description = "Reportes de ingresos, asistencias, rechazos y métricas administrativas")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/ingresos")
    @Operation(summary = "Ingresos por día", description = "Lista los ingresos agregados por día, tipo de pago y método de pago.")
    public List<IngresosDiariosItem> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return reportService.ingresosDiarios(desde, hasta);
    }

    @GetMapping("/ingresos/resumen")
    @Operation(summary = "Resumen de ingresos", description = "Suma total de ingresos en el periodo.")
    public BigDecimal resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return reportService.resumenIngresos(desde, hasta);
    }

    @GetMapping(value = "/ingresos.csv", produces = "text/csv")
    @Operation(summary = "Exportar ingresos CSV", description = "Exporta el reporte de ingresos en formato CSV.")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        List<IngresosDiariosItem> items = reportService.ingresosDiarios(desde, hasta);
        StringJoiner sj = new StringJoiner("\n");
        sj.add("dia,tipo_pago,metodo_pago,cantidad,total");
        for (IngresosDiariosItem i : items) {
            sj.add(String.format("%s,%s,%s,%d,%s",
                    i.getDia(), i.getTipoPago(), i.getMetodoPago(), i.getCantidad(), i.getTotal()));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ingresos.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(sj.toString());
    }

    // ==================== FASE 3.3: Reportes Adicionales ====================

    @GetMapping("/asistencias")
    @Operation(summary = "Asistencias diarias", description = "Lista asistencias (accesos exitosos) agregadas por día.")
    public List<AsistenciasDiariasItem> asistencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return reportService.asistenciasDiarias(desde, hasta);
    }

    @GetMapping("/rechazos")
    @Operation(summary = "Rechazos diarios", description = "Lista rechazos (accesos fallidos) agregados por día y motivo.")
    public List<RechazosDiariosItem> rechazos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return reportService.rechazosDiarios(desde, hasta);
    }

    @GetMapping("/derechos-activos")
    @Operation(summary = "Derechos de uso activos", description = "Lista derechos de uso vigentes con información de usuarios.")
    public List<DerechoActivoItem> derechosActivos() {
        return reportService.derechosActivos();
    }

    @GetMapping(value = "/asistencias.csv", produces = "text/csv")
    @Operation(summary = "Exportar asistencias CSV", description = "Exporta el reporte de asistencias en formato CSV.")
    public ResponseEntity<String> exportAsistenciasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        List<AsistenciasDiariasItem> items = reportService.asistenciasDiarias(desde, hasta);
        StringJoiner sj = new StringJoiner("\n");
        sj.add("dia,total_accesos,usuarios_unicos");
        for (AsistenciasDiariasItem i : items) {
            sj.add(String.format("%s,%d,%d", i.getDia(), i.getTotalAccesos(), i.getUsuariosUnicos()));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=asistencias.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(sj.toString());
    }

    @GetMapping(value = "/rechazos.csv", produces = "text/csv")
    @Operation(summary = "Exportar rechazos CSV", description = "Exporta el reporte de rechazos en formato CSV.")
    public ResponseEntity<String> exportRechazosCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        List<RechazosDiariosItem> items = reportService.rechazosDiarios(desde, hasta);
        StringJoiner sj = new StringJoiner("\n");
        sj.add("dia,motivo_rechazo,cantidad");
        for (RechazosDiariosItem i : items) {
            sj.add(String.format("%s,%s,%d", i.getDia(), i.getMotivoRechazo(), i.getCantidad()));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rechazos.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(sj.toString());
    }

    @GetMapping(value = "/derechos-activos.csv", produces = "text/csv")
    @Operation(summary = "Exportar derechos activos CSV", description = "Exporta el reporte de derechos activos en formato CSV.")
    public ResponseEntity<String> exportDerechosActivosCsv() {
        List<DerechoActivoItem> items = reportService.derechosActivos();
        StringJoiner sj = new StringJoiner("\n");
        sj.add("documento,nombre,tipo_derecho,vigente_desde,vigente_hasta,dias_restantes");
        for (DerechoActivoItem i : items) {
            sj.add(String.format("%s,%s,%s,%s,%s,%s",
                    i.getUsuarioDocumento(),
                    i.getUsuarioNombre(),
                    i.getTipoDerecho(),
                    i.getVigenteDesde(),
                    i.getVigenteHasta(),
                    i.getDiasRestantes() != null ? i.getDiasRestantes() : ""));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=derechos_activos.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(sj.toString());
    }
}
