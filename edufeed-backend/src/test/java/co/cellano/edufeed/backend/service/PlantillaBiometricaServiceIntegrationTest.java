package co.cellano.edufeed.backend.service;

import static org.assertj.core.api.Assertions.*;

import co.cellano.edufeed.backend.exception.BiometricEnrollmentException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de integración para PlantillaBiometricaService.
 * FASE 2.1: Tests de cifrado/descifrado AES-256-GCM.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "biometric.encryption.key=dGVzdC1rZXktMjU2LWJpdHMtZm9yLWRldmVsb3BtZW50LXB1cnBvc2VzLTEyMzQ1Njc4OTA="
})
@DisplayName("PlantillaBiometricaService - Tests de Cifrado")
class PlantillaBiometricaServiceIntegrationTest {

    @Autowired
    private PlantillaBiometricaService plantillaBiometricaService;

    @Autowired
    private PlantillaBiometricaRepository plantillaBiometricaRepository;

    private Usuario usuario;
    private byte[] datosOriginales;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setDocumento("1234567890");
        usuario.setNombreCompleto("Juan Pérez");

        // Datos de prueba (simulando una plantilla biométrica)
        datosOriginales = "PLANTILLA_BIOMETRICA_TEST_DATA_123456".getBytes();
    }

    // ========== TESTS CIFRADO/DESCIFRADO ==========

    @Test
    @DisplayName("CIFRAR: Debe cifrar y descifrar plantilla correctamente")
    void testCifrarDescifrar_Success() {
        // Given
        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.HUELLA);
        plantilla.setProveedor("test-provider");
        plantilla.setPlantilla(datosOriginales);

        // When - Cifrar y guardar
        PlantillaBiometrica guardada = plantillaBiometricaService.almacenarCifrada(plantilla);

        // Then - Verificar que se cifró (datos diferentes)
        assertThat(guardada).isNotNull();
        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getPlantilla()).isNotEqualTo(datosOriginales);
        assertThat(guardada.getPlantilla().length).isGreaterThan(datosOriginales.length); // IV + ciphertext

        // When - Descifrar
        PlantillaBiometrica descifrada = plantillaBiometricaService.recuperarDescifrada(guardada.getId());

        // Then - Verificar que se descifró correctamente
        assertThat(descifrada).isNotNull();
        assertThat(descifrada.getPlantilla()).isEqualTo(datosOriginales);
        assertThat(new String(descifrada.getPlantilla())).isEqualTo(new String(datosOriginales));
    }

    @Test
    @DisplayName("CIFRAR: Debe generar diferentes textos cifrados con mismo dato (por IV aleatorio)")
    void testCifrar_IVAleatorio() {
        // Given
        PlantillaBiometrica plantilla1 = new PlantillaBiometrica();
        plantilla1.setUsuario(usuario);
        plantilla1.setModalidad(Modalidad.HUELLA);
        plantilla1.setPlantilla(datosOriginales);

        PlantillaBiometrica plantilla2 = new PlantillaBiometrica();
        plantilla2.setUsuario(usuario);
        plantilla2.setModalidad(Modalidad.HUELLA);
        plantilla2.setPlantilla(datosOriginales);

        // When
        PlantillaBiometrica guardada1 = plantillaBiometricaService.almacenarCifrada(plantilla1);
        PlantillaBiometrica guardada2 = plantillaBiometricaService.almacenarCifrada(plantilla2);

        // Then - Los textos cifrados deben ser diferentes (por IV aleatorio)
        assertThat(guardada1.getPlantilla()).isNotEqualTo(guardada2.getPlantilla());

        // Pero al descifrar, deben dar los mismos datos originales
        PlantillaBiometrica descifrada1 = plantillaBiometricaService.recuperarDescifrada(guardada1.getId());
        PlantillaBiometrica descifrada2 = plantillaBiometricaService.recuperarDescifrada(guardada2.getId());

        assertThat(descifrada1.getPlantilla()).isEqualTo(descifrada2.getPlantilla());
    }

    @Test
    @DisplayName("CIFRAR: Debe lanzar excepción si plantilla está vacía")
    void testCifrar_PlantillaVacia() {
        // Given
        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.HUELLA);
        plantilla.setPlantilla(new byte[0]); // Plantilla vacía

        // When & Then
        assertThatThrownBy(() -> plantillaBiometricaService.almacenarCifrada(plantilla))
                .isInstanceOf(BiometricEnrollmentException.class)
                .hasMessageContaining("plantilla biométrica está vacía");
    }

    @Test
    @DisplayName("CIFRAR: Debe lanzar excepción si plantilla es null")
    void testCifrar_PlantillaNull() {
        // Given
        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.HUELLA);
        plantilla.setPlantilla(null); // Plantilla null

        // When & Then
        assertThatThrownBy(() -> plantillaBiometricaService.almacenarCifrada(plantilla))
                .isInstanceOf(BiometricEnrollmentException.class)
                .hasMessageContaining("plantilla biométrica está vacía");
    }

    @Test
    @DisplayName("DESCIFRAR: Debe lanzar ResourceNotFoundException si plantilla no existe")
    void testDescifrar_PlantillaNoExiste() {
        // Given
        UUID idInexistente = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> plantillaBiometricaService.recuperarDescifrada(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PlantillaBiometrica");
    }

    @Test
    @DisplayName("DESACTIVAR: Debe desactivar plantilla correctamente")
    void testDesactivar_Success() {
        // Given
        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.HUELLA);
        plantilla.setPlantilla(datosOriginales);

        PlantillaBiometrica guardada = plantillaBiometricaService.almacenarCifrada(plantilla);
        assertThat(guardada.isActivo()).isTrue();

        // When
        plantillaBiometricaService.desactivar(guardada.getId());

        // Then
        Optional<PlantillaBiometrica> desactivada = plantillaBiometricaRepository.findById(guardada.getId());
        assertThat(desactivada).isPresent();
        assertThat(desactivada.get().isActivo()).isFalse();
    }

    @Test
    @DisplayName("DESACTIVAR: Debe lanzar ResourceNotFoundException si plantilla no existe")
    void testDesactivar_PlantillaNoExiste() {
        // Given
        UUID idInexistente = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> plantillaBiometricaService.desactivar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PlantillaBiometrica");
    }

    @Test
    @DisplayName("CIFRAR: Debe cifrar datos grandes correctamente")
    void testCifrar_DatosGrandes() {
        // Given - Simular plantilla grande (10KB)
        byte[] datosGrandes = new byte[10240];
        Arrays.fill(datosGrandes, (byte) 0xAB);

        PlantillaBiometrica plantilla = new PlantillaBiometrica();
        plantilla.setUsuario(usuario);
        plantilla.setModalidad(Modalidad.ROSTRO);
        plantilla.setPlantilla(datosGrandes);

        // When
        PlantillaBiometrica guardada = plantillaBiometricaService.almacenarCifrada(plantilla);
        PlantillaBiometrica descifrada = plantillaBiometricaService.recuperarDescifrada(guardada.getId());

        // Then
        assertThat(descifrada.getPlantilla()).isEqualTo(datosGrandes);
        assertThat(descifrada.getPlantilla().length).isEqualTo(10240);
    }

    // ========== TEST GENERACIÓN DE CLAVE ==========

    @Test
    @DisplayName("GENERAR_CLAVE: Debe generar clave válida en Base64")
    void testGenerarClave() {
        // When
        String clave = PlantillaBiometricaService.generarClave();

        // Then
        assertThat(clave).isNotNull();
        assertThat(clave).isNotBlank();

        // Verificar que es Base64 válido y tiene 32 bytes (256 bits)
        byte[] claveBytes = java.util.Base64.getDecoder().decode(clave);
        assertThat(claveBytes.length).isEqualTo(32);
    }
}
