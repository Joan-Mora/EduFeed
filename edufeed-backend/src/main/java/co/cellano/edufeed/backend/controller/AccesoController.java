package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.AccesoDto;
import co.cellano.edufeed.backend.dto.request.AccesoCheckRequest;
import co.cellano.edufeed.backend.dto.response.AccesoCheckResponse;
import co.cellano.edufeed.backend.model.enums.EstadoAcceso;
import co.cellano.edufeed.backend.service.AccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para control de acceso y verificación de derechos.
 * 
 * <p>
 * Endpoints disponibles:
 * <ul>
 * <li>POST /api/accesos/verificar - Verificar derecho y registrar acceso</li>
 * <li>GET /api/accesos/historial - Consultar historial de accesos</li>
 * <li>GET /api/accesos/usuario/{usuarioId}/dia - Accesos de un usuario en un
 * día</li>
 * </ul>
 * </p>
 * 
 * @since FASE 2.3
 */
@RestController
@RequestMapping("/api/accesos")
@Tag(name = "Accesos", description = "Control de acceso y verificación de derechos de uso")
public class AccesoController {

    private final AccesoService accesoService;

    public AccesoController(AccesoService accesoService) {
        this.accesoService = accesoService;
    }

    /**
     * Verifica el derecho de acceso de un usuario y registra el intento.
     * 
     * @param request Solicitud con usuarioId y modalidad biométrica
     * @return Respuesta con resultado de verificación (permitido/denegado)
     */
    @PostMapping("/verificar")
        @PreAuthorize("hasAnyRole('OPERADOR_ACCESO','SUPERVISOR','ADMIN')")
    @Operation(summary = "Verificar derecho de acceso", description = "Verifica si un usuario tiene derecho vigente para acceder y registra el intento. "
            +
            "Retorna orientación a caja si se deniega el acceso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación exitosa (puede ser permitido o denegado)", content = @Content(schema = @Schema(implementation = AccesoCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<AccesoCheckResponse> verificarAcceso(
            @Valid @RequestBody AccesoCheckRequest request) {
        AccesoCheckResponse response = accesoService.verificarAcceso(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene el historial de accesos con filtros opcionales.
     * 
     * @param usuarioId ID del usuario (opcional)
     * @param inicio    Fecha/hora de inicio (opcional)
     * @param fin       Fecha/hora de fin (opcional)
     * @param estado    Estado del acceso (opcional)
     * @param page      Número de página (default: 0)
     * @param size      Tamaño de página (default: 20)
     * @param sort      Ordenamiento (default: fechaHora,desc)
     * @return Página con accesos que cumplen los filtros
     */
    @GetMapping("/historial")
        @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @Operation(summary = "Consultar historial de accesos", description = "Obtiene el historial de accesos con filtros opcionales: usuario, rango de fechas, estado. "
            +
            "Los resultados están paginados y ordenados por fecha descendente por defecto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos")
    })
    public ResponseEntity<Page<AccesoDto>> obtenerHistorial(
            @Parameter(description = "ID del usuario a filtrar") @RequestParam(required = false) UUID usuarioId,

            @Parameter(description = "Fecha/hora de inicio del rango (formato ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,

            @Parameter(description = "Fecha/hora de fin del rango (formato ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fin,

            @Parameter(description = "Estado del acceso (APROBADO o DENEGADO)") @RequestParam(required = false) EstadoAcceso estado,

            @Parameter(description = "Número de página (inicia en 0)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Ordenamiento (campo,dirección). Ej: fechaHora,desc") @RequestParam(defaultValue = "fechaHora,desc") String sort) {

        // Parsear ordenamiento
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        Page<AccesoDto> historial = accesoService.obtenerHistorial(
                usuarioId, inicio, fin, estado, pageable);

        return ResponseEntity.ok(historial);
    }

    /**
     * Obtiene los accesos de un usuario en un día específico.
     * 
     * @param usuarioId ID del usuario
     * @param fecha     Fecha a consultar (formato ISO-8601)
     * @return Lista de accesos del usuario en esa fecha
     */
    @GetMapping("/usuario/{usuarioId}/dia")
        @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @Operation(summary = "Accesos de usuario por día", description = "Obtiene todos los accesos de un usuario en un día específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Accesos obtenidos exitosamente", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<List<AccesoDto>> obtenerAccesosPorDia(
            @Parameter(description = "ID del usuario") @PathVariable("usuarioId") UUID usuarioId,

            @Parameter(description = "Fecha a consultar (formato ISO-8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fecha) {

        List<AccesoDto> accesos = accesoService.obtenerAccesosPorDia(usuarioId, fecha);
        return ResponseEntity.ok(accesos);
    }
}
