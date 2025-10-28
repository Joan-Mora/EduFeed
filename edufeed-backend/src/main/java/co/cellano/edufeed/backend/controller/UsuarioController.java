package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.PlantillaBiometricaDto;
import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.dto.request.BiometricEnrollRequest;
import co.cellano.edufeed.backend.mapper.PlantillaBiometricaMapper;
import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import co.cellano.edufeed.backend.service.BiometricService;
import co.cellano.edufeed.backend.service.PlantillaBiometricaService;
import co.cellano.edufeed.backend.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión de usuarios.
 * FASE 2.1: CRUD completo con validaciones.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final BiometricService biometricService;
    private final PlantillaBiometricaService plantillaBiometricaService;
    private final PlantillaBiometricaRepository plantillaBiometricaRepository;

    public UsuarioController(UsuarioService usuarioService,
                             BiometricService biometricService,
                             PlantillaBiometricaService plantillaBiometricaService,
                             PlantillaBiometricaRepository plantillaBiometricaRepository) {
        this.usuarioService = usuarioService;
        this.biometricService = biometricService;
        this.plantillaBiometricaService = plantillaBiometricaService;
        this.plantillaBiometricaRepository = plantillaBiometricaRepository;
    }

    /**
     * Crea un nuevo usuario.
     * POST /api/usuarios
     */
    @PostMapping
    public ResponseEntity<UsuarioDto> create(@Valid @RequestBody UsuarioDto dto) {
        UsuarioDto created = usuarioService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza un usuario existente.
     * PUT /api/usuarios/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDto> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UsuarioDto dto) {
        UsuarioDto updated = usuarioService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Desactiva un usuario (soft delete).
     * DELETE /api/usuarios/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable("id") UUID id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva un usuario previamente desactivado.
     * POST /api/usuarios/{id}/reactivar
     */
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable("id") UUID id) {
        usuarioService.reactivar(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Lista todos los usuarios (opcionalmente solo activos).
     * GET /api/usuarios?soloActivos=true
     */
    @GetMapping
    public List<UsuarioDto> list(@RequestParam(value = "soloActivos", required = false) Boolean soloActivos) {
        if (soloActivos != null && soloActivos) {
            return usuarioService.listActivos();
        }
        return usuarioService.list();
    }

    /**
     * Lista usuarios con paginación.
     * GET /api/usuarios?page=0&size=20
     */
    @GetMapping(params = {"page", "size"})
    public Page<UsuarioDto> listPaged(@RequestParam("page") int page, @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return usuarioService.list(pageable);
    }

    /**
     * Obtiene un usuario por ID.
     * GET /api/usuarios/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDto> get(@PathVariable("id") UUID id) {
        UsuarioDto dto = usuarioService.get(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Busca usuarios por documento.
     * GET /api/usuarios/buscar/documento/{documento}
     */
    @GetMapping("/buscar/documento/{documento}")
    public ResponseEntity<UsuarioDto> buscarPorDocumento(@PathVariable("documento") String documento) {
        UsuarioDto dto = usuarioService.buscarPorDocumento(documento);
        return ResponseEntity.ok(dto);
    }

    /**
     * Busca usuarios por nombre (búsqueda parcial).
     * GET /api/usuarios/buscar/nombre?q=Juan
     */
    @GetMapping("/buscar/nombre")
    public List<UsuarioDto> buscarPorNombre(@RequestParam("q") String nombreParcial) {
        return usuarioService.buscarPorNombre(nombreParcial);
    }

    /**
     * Busca usuarios por tipo.
     * GET /api/usuarios/buscar/tipo/{tipo}
     */
    @GetMapping("/buscar/tipo/{tipo}")
    public List<UsuarioDto> buscarPorTipo(@PathVariable("tipo") TipoUsuario tipo) {
        return usuarioService.buscarPorTipo(tipo);
    }

    // --------- BIOMETRÍA POR USUARIO ---------

    /**
     * Enrola una nueva plantilla biométrica para el usuario indicado.
     * POST /api/usuarios/{id}/biometria/enrolar
     */
    @PostMapping("/{id}/biometria/enrolar")
    public ResponseEntity<PlantillaBiometricaDto> enrolarBiometria(
            @PathVariable("id") UUID id,
            @Valid @RequestBody BiometricEnrollRequest request) {
        PlantillaBiometrica plantilla = biometricService.enrolar(id, request.getModalidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlantillaBiometricaMapper.toDto(plantilla));
    }

    /**
     * Lista plantillas biométricas activas del usuario.
     * GET /api/usuarios/{id}/biometria
     */
    @GetMapping("/{id}/biometria")
    public List<PlantillaBiometricaDto> listarBiometrias(@PathVariable("id") UUID id) {
        // Para evitar exponer bytes, devolvemos solo metadatos
    return plantillaBiometricaRepository.findByUsuarioIdAndActivoTrue(id)
                .stream()
                .map(PlantillaBiometricaMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Desactiva una plantilla biométrica del usuario.
     * DELETE /api/usuarios/{id}/biometria/{plantillaId}
     */
    @DeleteMapping("/{id}/biometria/{plantillaId}")
    public ResponseEntity<Void> desactivarBiometria(@PathVariable("id") UUID id,
                                                    @PathVariable("plantillaId") UUID plantillaId) {
        // No necesitamos validar usuario vs plantilla aquí; el servicio valida existencia
        plantillaBiometricaService.desactivar(plantillaId);
        return ResponseEntity.noContent().build();
    }
}
