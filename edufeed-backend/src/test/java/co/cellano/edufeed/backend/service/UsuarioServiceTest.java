package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.exception.DuplicateDocumentException;
import co.cellano.edufeed.backend.exception.InvalidBusinessRuleException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UsuarioService.
 * FASE 2.1: Cobertura ≥80% con Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Tests de CRUD completo")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioEntity;
    private UsuarioDto usuarioDto;

    @BeforeEach
    void setUp() {
        usuarioEntity = new Usuario();
        usuarioEntity.setId(UUID.randomUUID());
        usuarioEntity.setDocumento("1234567890");
        usuarioEntity.setNombreCompleto("Juan Pérez");
        usuarioEntity.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuarioEntity.setEmail("juan@example.com");
        usuarioEntity.setTelefono("3001234567");
        usuarioEntity.setActivo(true);

        usuarioDto = new UsuarioDto();
        usuarioDto.setDocumento("1234567890");
        usuarioDto.setNombreCompleto("Juan Pérez");
        usuarioDto.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuarioDto.setEmail("juan@example.com");
        usuarioDto.setTelefono("3001234567");
    }

    // ========== TESTS CREATE ==========

    @Test
    @DisplayName("CREATE: Debe crear usuario exitosamente")
    void testCreate_Success() {
        // Given
        when(usuarioRepository.findByDocumento("1234567890")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEntity);

        // When
        UsuarioDto result = usuarioService.create(usuarioDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumento()).isEqualTo("1234567890");
        assertThat(result.getNombreCompleto()).isEqualTo("Juan Pérez");
        verify(usuarioRepository, times(1)).findByDocumento("1234567890");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("CREATE: Debe lanzar DuplicateDocumentException si documento existe")
    void testCreate_DuplicateDocument() {
        // Given
        when(usuarioRepository.findByDocumento("1234567890")).thenReturn(Optional.of(usuarioEntity));

        // When & Then
        assertThatThrownBy(() -> usuarioService.create(usuarioDto))
                .isInstanceOf(DuplicateDocumentException.class)
                .hasMessageContaining("1234567890");

        verify(usuarioRepository, times(1)).findByDocumento("1234567890");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CREATE: Debe lanzar InvalidBusinessRuleException si email inválido")
    void testCreate_InvalidEmail() {
        // Given
        usuarioDto.setEmail("email-invalido");
        when(usuarioRepository.findByDocumento(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> usuarioService.create(usuarioDto))
                .isInstanceOf(InvalidBusinessRuleException.class)
                .hasMessageContaining("email");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CREATE: Debe lanzar InvalidBusinessRuleException si teléfono inválido")
    void testCreate_InvalidTelefono() {
        // Given
        usuarioDto.setTelefono("123"); // Demasiado corto
        when(usuarioRepository.findByDocumento(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> usuarioService.create(usuarioDto))
                .isInstanceOf(InvalidBusinessRuleException.class)
                .hasMessageContaining("teléfono");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CREATE: Debe aceptar teléfono con prefijo +57")
    void testCreate_TelefonoConPrefijo() {
        // Given
        usuarioDto.setTelefono("+573001234567");
        when(usuarioRepository.findByDocumento(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEntity);

        // When
        UsuarioDto result = usuarioService.create(usuarioDto);

        // Then
        assertThat(result).isNotNull();
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    // ========== TESTS UPDATE ==========

    @Test
    @DisplayName("UPDATE: Debe actualizar usuario exitosamente")
    void testUpdate_Success() {
        // Given
        UUID id = usuarioEntity.getId();
        usuarioDto.setNombreCompleto("Juan Carlos Pérez");
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEntity);

        // When
        UsuarioDto result = usuarioService.update(id, usuarioDto);

        // Then
        assertThat(result).isNotNull();
        verify(usuarioRepository, times(1)).findById(id);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("UPDATE: Debe lanzar ResourceNotFoundException si usuario no existe")
    void testUpdate_NotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> usuarioService.update(id, usuarioDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario");

        verify(usuarioRepository, times(1)).findById(id);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UPDATE: Debe lanzar DuplicateDocumentException si nuevo documento existe")
    void testUpdate_DuplicateDocument() {
        // Given
        UUID id = usuarioEntity.getId();
        usuarioDto.setDocumento("9999999999"); // Documento diferente

        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(UUID.randomUUID());
        otroUsuario.setDocumento("9999999999");

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.findByDocumento("9999999999")).thenReturn(Optional.of(otroUsuario));

        // When & Then
        assertThatThrownBy(() -> usuarioService.update(id, usuarioDto))
                .isInstanceOf(DuplicateDocumentException.class)
                .hasMessageContaining("9999999999");
    }

    // ========== TESTS DESACTIVAR/REACTIVAR ==========

    @Test
    @DisplayName("DESACTIVAR: Debe desactivar usuario exitosamente")
    void testDesactivar_Success() {
        // Given
        UUID id = usuarioEntity.getId();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEntity);

        // When
        usuarioService.desactivar(id);

        // Then
        assertThat(usuarioEntity.isActivo()).isFalse();
        verify(usuarioRepository, times(1)).findById(id);
        verify(usuarioRepository, times(1)).save(usuarioEntity);
    }

    @Test
    @DisplayName("DESACTIVAR: Debe lanzar ResourceNotFoundException si usuario no existe")
    void testDesactivar_NotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> usuarioService.desactivar(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("REACTIVAR: Debe reactivar usuario exitosamente")
    void testReactivar_Success() {
        // Given
        UUID id = usuarioEntity.getId();
        usuarioEntity.setActivo(false);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEntity);

        // When
        usuarioService.reactivar(id);

        // Then
        assertThat(usuarioEntity.isActivo()).isTrue();
        verify(usuarioRepository, times(1)).save(usuarioEntity);
    }

    // ========== TESTS LIST ==========

    @Test
    @DisplayName("LIST: Debe listar todos los usuarios")
    void testList_Success() {
        // Given
        Usuario usuario2 = new Usuario();
        usuario2.setId(UUID.randomUUID());
        usuario2.setDocumento("9999999999");
        usuario2.setNombreCompleto("María López");
        usuario2.setTipoUsuario(TipoUsuario.DOCENTE);
        usuario2.setActivo(false);

        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuarioEntity, usuario2));

        // When
        List<UsuarioDto> result = usuarioService.list();

        // Then
        assertThat(result).hasSize(2);
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("LIST_ACTIVOS: Debe listar solo usuarios activos")
    void testListActivos_Success() {
        // Given
        Usuario usuario2 = new Usuario();
        usuario2.setId(UUID.randomUUID());
        usuario2.setActivo(false);

        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuarioEntity, usuario2));

        // When
        List<UsuarioDto> result = usuarioService.listActivos();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocumento()).isEqualTo("1234567890");
    }

    // ========== TESTS BÚSQUEDA ==========

    @Test
    @DisplayName("BUSCAR_POR_DOCUMENTO: Debe encontrar usuario por documento")
    void testBuscarPorDocumento_Success() {
        // Given
        when(usuarioRepository.findByDocumento("1234567890")).thenReturn(Optional.of(usuarioEntity));

        // When
        UsuarioDto result = usuarioService.buscarPorDocumento("1234567890");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumento()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("BUSCAR_POR_DOCUMENTO: Debe lanzar ResourceNotFoundException si no existe")
    void testBuscarPorDocumento_NotFound() {
        // Given
        when(usuarioRepository.findByDocumento("0000000000")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> usuarioService.buscarPorDocumento("0000000000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("0000000000");
    }

    @Test
    @DisplayName("BUSCAR_POR_NOMBRE: Debe encontrar usuarios por nombre parcial")
    void testBuscarPorNombre_Success() {
        // Given
        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuarioEntity));

        // When
        List<UsuarioDto> result = usuarioService.buscarPorNombre("juan");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombreCompleto()).containsIgnoringCase("juan");
    }

    @Test
    @DisplayName("BUSCAR_POR_TIPO: Debe encontrar usuarios por tipo")
    void testBuscarPorTipo_Success() {
        // Given
        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuarioEntity));

        // When
        List<UsuarioDto> result = usuarioService.buscarPorTipo(TipoUsuario.ESTUDIANTE);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipoUsuario()).isEqualTo(TipoUsuario.ESTUDIANTE);
    }

    @Test
    @DisplayName("GET: Debe obtener usuario por ID")
    void testGet_Success() {
        // Given
        UUID id = usuarioEntity.getId();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEntity));

        // When
        UsuarioDto result = usuarioService.get(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumento()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("GET: Debe lanzar ResourceNotFoundException si usuario no existe")
    void testGet_NotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> usuarioService.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
