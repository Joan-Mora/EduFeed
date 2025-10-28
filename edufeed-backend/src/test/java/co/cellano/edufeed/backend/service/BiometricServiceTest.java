package co.cellano.edufeed.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import co.cellano.edufeed.backend.exception.BiometricEnrollmentException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import co.cellano.edufeed.backend.repository.UsuarioRepository;
import co.cellano.edufeed.biometric.BiometricProvider;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios para BiometricService.
 * FASE 2.1: Cobertura de enrolamiento y verificación biométrica.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BiometricService - Tests de Biometría")
class BiometricServiceTest {

    @Mock
    private BiometricProvider biometricProvider;

    @Mock
    private PlantillaBiometricaService plantillaBiometricaService;

    @Mock
    private PlantillaBiometricaRepository plantillaBiometricaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private BiometricService biometricService;

    private Usuario usuario;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setDocumento("1234567890");
        usuario.setNombreCompleto("Juan Pérez");
        usuario.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuario.setActivo(true);
    }

    // ========== TESTS ENROLAMIENTO ==========

    @Test
    @DisplayName("ENROLAR: Debe enrolar plantilla exitosamente")
    void testEnrolar_Success() {
        // Given
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        BiometricProvider.EnrollmentResult enrollmentResult = new BiometricProvider.EnrollmentResult(true,
                usuarioId.toString(), "MOCK enrolled FINGERPRINT");
        when(biometricProvider.enroll(anyString(), any(BiometricProvider.Modality.class)))
                .thenReturn(enrollmentResult);

        when(biometricProvider.getVersion()).thenReturn(Optional.of("mock-1.0"));

        PlantillaBiometrica plantillaGuardada = new PlantillaBiometrica();
        plantillaGuardada.setId(UUID.randomUUID());
        plantillaGuardada.setUsuario(usuario);
        plantillaGuardada.setModalidad(Modalidad.HUELLA);
        when(plantillaBiometricaService.almacenarCifrada(any(PlantillaBiometrica.class)))
                .thenReturn(plantillaGuardada);

        // When
        PlantillaBiometrica resultado = biometricService.enrolar(usuarioId, Modalidad.HUELLA);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getModalidad()).isEqualTo(Modalidad.HUELLA);

        verify(usuarioRepository, times(1)).findById(usuarioId);
        verify(biometricProvider, times(1)).enroll(eq(usuarioId.toString()),
                eq(BiometricProvider.Modality.FINGERPRINT));
        verify(plantillaBiometricaService, times(1)).almacenarCifrada(any(PlantillaBiometrica.class));
    }

    @Test
    @DisplayName("ENROLAR: Debe lanzar ResourceNotFoundException si usuario no existe")
    void testEnrolar_UsuarioNoExiste() {
        // Given
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> biometricService.enrolar(usuarioId, Modalidad.HUELLA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario");

        verify(biometricProvider, never()).enroll(anyString(), any());
    }

    @Test
    @DisplayName("ENROLAR: Debe lanzar BiometricEnrollmentException si enrolamiento falla")
    void testEnrolar_EnrollmentFallido() {
        // Given
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        BiometricProvider.EnrollmentResult enrollmentResult = new BiometricProvider.EnrollmentResult(false,
                usuarioId.toString(), "Device not found");
        when(biometricProvider.enroll(anyString(), any(BiometricProvider.Modality.class)))
                .thenReturn(enrollmentResult);

        // When & Then
        assertThatThrownBy(() -> biometricService.enrolar(usuarioId, Modalidad.HUELLA))
                .isInstanceOf(BiometricEnrollmentException.class)
                .hasMessageContaining("Enrolamiento fallido");

        verify(plantillaBiometricaService, never()).almacenarCifrada(any());
    }

    @Test
    @DisplayName("ENROLAR: Debe enrolar con diferentes modalidades")
    void testEnrolar_DiferentesModalidades() {
        // Given
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        BiometricProvider.EnrollmentResult enrollmentResult = new BiometricProvider.EnrollmentResult(true,
                usuarioId.toString(), "MOCK enrolled");
        when(biometricProvider.enroll(anyString(), any(BiometricProvider.Modality.class)))
                .thenReturn(enrollmentResult);

        when(biometricProvider.getVersion()).thenReturn(Optional.of("mock-1.0"));

        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setId(UUID.randomUUID());
        when(plantillaBiometricaService.almacenarCifrada(any(PlantillaBiometrica.class)))
                .thenReturn(plantilla);

        // When & Then - HUELLA
        PlantillaBiometrica resultadoHuella = biometricService.enrolar(usuarioId, Modalidad.HUELLA);
        assertThat(resultadoHuella).isNotNull();
        verify(biometricProvider).enroll(eq(usuarioId.toString()), eq(BiometricProvider.Modality.FINGERPRINT));

        // When & Then - ROSTRO
        PlantillaBiometrica resultadoRostro = biometricService.enrolar(usuarioId, Modalidad.ROSTRO);
        assertThat(resultadoRostro).isNotNull();
        verify(biometricProvider).enroll(eq(usuarioId.toString()), eq(BiometricProvider.Modality.FACE));

        // When & Then - VOZ
        PlantillaBiometrica resultadoVoz = biometricService.enrolar(usuarioId, Modalidad.VOZ);
        assertThat(resultadoVoz).isNotNull();
        verify(biometricProvider).enroll(eq(usuarioId.toString()), eq(BiometricProvider.Modality.VOICE));
    }

    // ========== TESTS VERIFICACIÓN 1:1 ==========

    @Test
    @DisplayName("VERIFICAR_1A1: Debe verificar exitosamente con plantilla existente")
    void testVerificar1a1_Success() {
        // Given
        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setId(UUID.randomUUID());
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.HUELLA);
        plantilla.setActivo(true);

        when(plantillaBiometricaRepository.findAll()).thenReturn(Arrays.asList(plantilla));
        when(plantillaBiometricaService.recuperarDescifrada(any())).thenReturn(plantilla);

        BiometricProvider.VerificationResult verificationResult = new BiometricProvider.VerificationResult(true,
                usuarioId.toString(), 0.95, "MOCK verified");
        when(biometricProvider.verify(any(BiometricProvider.Modality.class)))
                .thenReturn(verificationResult);

        // When
        BiometricService.VerificationResult resultado = biometricService.verificar1a1(usuarioId, Modalidad.HUELLA);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.success()).isTrue();
        assertThat(resultado.usuarioId()).isEqualTo(usuarioId);
        assertThat(resultado.score()).isEqualTo(0.95);

        verify(plantillaBiometricaRepository, times(1)).findAll();
        verify(biometricProvider, times(1)).verify(eq(BiometricProvider.Modality.FINGERPRINT));
    }

    @Test
    @DisplayName("VERIFICAR_1A1: Debe fallar si no existe plantilla activa")
    void testVerificar1a1_SinPlantilla() {
        // Given
        when(plantillaBiometricaRepository.findAll()).thenReturn(Arrays.asList());

        // When & Then
        assertThatThrownBy(() -> biometricService.verificar1a1(usuarioId, Modalidad.HUELLA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PlantillaBiometrica");
    }

    @Test
    @DisplayName("VERIFICAR_1A1: Debe retornar false si score es bajo")
    void testVerificar1a1_ScoreBajo() {
        // Given
        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setId(UUID.randomUUID());
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.HUELLA);
        plantilla.setActivo(true);

        when(plantillaBiometricaRepository.findAll()).thenReturn(Arrays.asList(plantilla));
        when(plantillaBiometricaService.recuperarDescifrada(any())).thenReturn(plantilla);

        BiometricProvider.VerificationResult verificationResult = new BiometricProvider.VerificationResult(true,
                usuarioId.toString(), 0.50, "Low confidence");
        when(biometricProvider.verify(any(BiometricProvider.Modality.class)))
                .thenReturn(verificationResult);

        // When
        BiometricService.VerificationResult resultado = biometricService.verificar1a1(usuarioId, Modalidad.HUELLA);

        // Then
        assertThat(resultado.success()).isFalse(); // Score 0.50 < 0.70 (threshold)
        assertThat(resultado.score()).isEqualTo(0.50);
    }

    // ========== TESTS VERIFICACIÓN 1:N ==========

    @Test
    @DisplayName("VERIFICAR_1AN: Debe encontrar usuario con plantilla coincidente")
    void testVerificar1aN_UsuarioEncontrado() {
        // Given
        PlantillaBiometrica plantilla1 = new PlantillaBiometrica();
        plantilla1.setId(UUID.randomUUID());
        plantilla1.setUsuario(usuario);
        plantilla1.setModalidad(Modalidad.HUELLA);
        plantilla1.setActivo(true);

        Usuario usuario2 = new Usuario();
        usuario2.setId(UUID.randomUUID());
        PlantillaBiometrica plantilla2 = new PlantillaBiometrica();
        plantilla2.setId(UUID.randomUUID());
        plantilla2.setUsuario(usuario2);
        plantilla2.setModalidad(Modalidad.HUELLA);
        plantilla2.setActivo(true);

        when(plantillaBiometricaRepository.findAll()).thenReturn(Arrays.asList(plantilla1, plantilla2));

        BiometricProvider.VerificationResult verificationResult = new BiometricProvider.VerificationResult(true,
                usuarioId.toString(), 0.90, "MOCK verified");
        when(biometricProvider.verify(any(BiometricProvider.Modality.class)))
                .thenReturn(verificationResult);

        // When
        BiometricService.VerificationResult resultado = biometricService.verificar1aN(Modalidad.HUELLA);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.success()).isTrue();
        assertThat(resultado.usuarioId()).isEqualTo(usuarioId);
        assertThat(resultado.score()).isEqualTo(0.90);
    }

    @Test
    @DisplayName("VERIFICAR_1AN: Debe retornar null si no hay coincidencias")
    void testVerificar1aN_SinCoincidencias() {
        // Given
        when(plantillaBiometricaRepository.findAll()).thenReturn(Arrays.asList());

        BiometricProvider.VerificationResult verificationResult = new BiometricProvider.VerificationResult(true,
                UUID.randomUUID().toString(), 0.85, "No match");
        when(biometricProvider.verify(any(BiometricProvider.Modality.class)))
                .thenReturn(verificationResult);

        // When
        BiometricService.VerificationResult resultado = biometricService.verificar1aN(Modalidad.HUELLA);

        // Then
        assertThat(resultado.success()).isFalse();
        assertThat(resultado.usuarioId()).isNull();
    }

    @Test
    @DisplayName("VERIFICAR_1AN: Debe fallar si captura biométrica falla")
    void testVerificar1aN_CapturaFallida() {
        // Given
        BiometricProvider.VerificationResult verificationResult = new BiometricProvider.VerificationResult(false, null,
                0.0, "Capture failed");
        when(biometricProvider.verify(any(BiometricProvider.Modality.class)))
                .thenReturn(verificationResult);

        // When
        BiometricService.VerificationResult resultado = biometricService.verificar1aN(Modalidad.HUELLA);

        // Then
        assertThat(resultado.success()).isFalse();
        assertThat(resultado.usuarioId()).isNull();
        assertThat(resultado.detail()).contains("Capture failed");
    }
}
