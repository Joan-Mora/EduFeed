package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para gestión de usuarios.
 * FASE 2.1: CRUD completo con validaciones.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
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
}
