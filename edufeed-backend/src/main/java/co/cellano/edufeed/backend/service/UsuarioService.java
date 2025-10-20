package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.exception.DuplicateDocumentException;
import co.cellano.edufeed.backend.exception.InvalidBusinessRuleException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.mapper.UsuarioMapper;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de usuarios con validaciones de negocio.
 * FASE 2.1: CRUD completo con validaciones, soft delete y búsqueda.
 */
@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    // Patrones de validación
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern TELEFONO_PATTERN = Pattern.compile("^\\+?57\\d{10}$|^\\d{10}$");

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Crea un nuevo usuario con validaciones de negocio.
     * Valida:
     * - Documento único (lanza DuplicateDocumentException si existe)
     * - Formato de email y teléfono
     * 
     * @param dto Datos del usuario a crear
     * @return UsuarioDto creado con ID generado
     * @throws DuplicateDocumentException   si el documento ya existe
     * @throws InvalidBusinessRuleException si los datos no cumplen reglas de
     *                                      negocio
     */
    public UsuarioDto create(UsuarioDto dto) {
        // Validar documento único
        if (usuarioRepository.findByDocumento(dto.getDocumento()).isPresent()) {
            throw new DuplicateDocumentException(dto.getDocumento());
        }

        // Validaciones de formato
        validateBusinessRules(dto);

        Usuario u = UsuarioMapper.toEntity(dto);
        u.setActivo(true); // Por defecto activo al crear
        Usuario saved = usuarioRepository.save(u);
        return UsuarioMapper.toDto(saved);
    }

    /**
     * Actualiza un usuario existente.
     * Valida que el documento no esté duplicado en otro usuario.
     * 
     * @param id  ID del usuario a actualizar
     * @param dto Datos actualizados
     * @return UsuarioDto actualizado
     * @throws ResourceNotFoundException  si el usuario no existe
     * @throws DuplicateDocumentException si el documento ya existe en otro usuario
     */
    public UsuarioDto update(UUID id, UsuarioDto dto) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        // Validar documento único (si cambió)
        if (!existing.getDocumento().equals(dto.getDocumento())) {
            if (usuarioRepository.findByDocumento(dto.getDocumento()).isPresent()) {
                throw new DuplicateDocumentException(dto.getDocumento());
            }
        }

        // Validaciones de formato
        validateBusinessRules(dto);

        // Actualizar campos
        existing.setDocumento(dto.getDocumento());
        existing.setNombreCompleto(dto.getNombreCompleto());
        existing.setTipoUsuario(dto.getTipoUsuario());
        existing.setEmail(dto.getEmail());
        existing.setTelefono(dto.getTelefono());
        // No actualizar 'activo' aquí, usar desactivar()

        Usuario updated = usuarioRepository.save(existing);
        return UsuarioMapper.toDto(updated);
    }

    /**
     * Desactiva un usuario (soft delete).
     * No elimina físicamente el registro, solo actualiza el campo activo.
     * 
     * @param id ID del usuario a desactivar
     * @throws ResourceNotFoundException si el usuario no existe
     */
    public void desactivar(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Reactiva un usuario previamente desactivado.
     * 
     * @param id ID del usuario a reactivar
     * @throws ResourceNotFoundException si el usuario no existe
     */
    public void reactivar(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    /**
     * Lista todos los usuarios.
     * 
     * @return Lista de UsuarioDto
     */
    @Transactional(readOnly = true)
    public List<UsuarioDto> list() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lista solo usuarios activos.
     * 
     * @return Lista de UsuarioDto activos
     */
    @Transactional(readOnly = true)
    public List<UsuarioDto> listActivos() {
        return usuarioRepository.findAll().stream()
                .filter(Usuario::isActivo)
                .map(UsuarioMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca usuarios por tipo.
     * 
     * @param tipo Tipo de usuario (NINO, ESTUDIANTE, DOCENTE, PERSONAL)
     * @return Lista de UsuarioDto del tipo especificado
     */
    @Transactional(readOnly = true)
    public List<UsuarioDto> buscarPorTipo(TipoUsuario tipo) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getTipoUsuario() == tipo)
                .map(UsuarioMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca un usuario por documento.
     * 
     * @param documento Documento del usuario
     * @return UsuarioDto si existe
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public UsuarioDto buscarPorDocumento(String documento) {
        return usuarioRepository.findByDocumento(documento)
                .map(UsuarioMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "documento", documento));
    }

    /**
     * Busca usuarios por nombre (búsqueda parcial case-insensitive).
     * 
     * @param nombreParcial Parte del nombre a buscar
     * @return Lista de UsuarioDto que coincidan
     */
    @Transactional(readOnly = true)
    public List<UsuarioDto> buscarPorNombre(String nombreParcial) {
        String pattern = nombreParcial.toLowerCase();
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getNombreCompleto().toLowerCase().contains(pattern))
                .map(UsuarioMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un usuario por ID.
     * 
     * @param id ID del usuario
     * @return UsuarioDto
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public UsuarioDto get(UUID id) {
        return usuarioRepository.findById(id)
                .map(UsuarioMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    /**
     * Valida las reglas de negocio para el usuario.
     * 
     * @param dto Datos a validar
     * @throws InvalidBusinessRuleException si no cumple las reglas
     */
    private void validateBusinessRules(UsuarioDto dto) {
        // Validar email si está presente
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
                throw new InvalidBusinessRuleException("EMAIL_INVALIDO",
                        "El formato del email es inválido: " + dto.getEmail());
            }
        }

        // Validar teléfono si está presente
        if (dto.getTelefono() != null && !dto.getTelefono().isBlank()) {
            if (!TELEFONO_PATTERN.matcher(dto.getTelefono()).matches()) {
                throw new InvalidBusinessRuleException("TELEFONO_INVALIDO",
                        "El teléfono debe tener formato colombiano (+57XXXXXXXXXX o 10 dígitos): " + dto.getTelefono());
            }
        }
    }
}
