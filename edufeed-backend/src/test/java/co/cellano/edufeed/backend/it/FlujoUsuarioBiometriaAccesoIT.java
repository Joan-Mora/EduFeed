package co.cellano.edufeed.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.dto.request.AccesoCheckRequest;
import co.cellano.edufeed.backend.dto.response.AccesoCheckResponse;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.service.AccesoService;
import co.cellano.edufeed.backend.service.BiometricService;
import co.cellano.edufeed.backend.service.UsuarioService;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Flujo E2E: Crear usuario → enrolar biometría → verificar acceso.
 * Resultado esperado: enrolamiento OK; verificación de acceso DENEGADA por ausencia de derecho vigente
 * (orientación a caja disponible). La verificación biométrica 1:1 puede variar según el mock, por lo que
 * mantenemos determinismo validando el resultado de acceso.
 */
class FlujoUsuarioBiometriaAccesoIT extends BaseIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private BiometricService biometricService;

    @Autowired
    private AccesoService accesoService;

    @Test
    @DisplayName("E2E: Usuario + Enrolamiento biométrico + Acceso (denegado sin derecho)")
    void flujoUsuarioEnrolamientoAcceso_denegadoPorFaltaDeDerecho() {
        // 1) Crear usuario activo
    UsuarioDto nuevo = new UsuarioDto();
    String doc = String.valueOf(ThreadLocalRandom.current().nextInt(900000000, 999999999));
    nuevo.setDocumento(doc);
        nuevo.setNombreCompleto("Estudiante Prueba");
        nuevo.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        nuevo.setEmail("est.prueba@example.com");
        nuevo.setTelefono("3001234567");
        UsuarioDto creado = usuarioService.create(nuevo);

        // 2) Enrolar biometría (huella)
        var plantilla = biometricService.enrolar(UUID.fromString(creado.getId()), Modalidad.HUELLA);
        assertThat(plantilla).isNotNull();
        assertThat(plantilla.getUsuario().getId()).isEqualTo(UUID.fromString(creado.getId()));

        // 3) Verificar acceso (debe ser denegado por no tener derecho vigente)
        AccesoCheckRequest req = new AccesoCheckRequest();
        req.setUsuarioId(UUID.fromString(creado.getId()));
        req.setModalidad(Modalidad.HUELLA);

    AccesoCheckResponse res = accesoService.verificarAcceso(req);
    assertThat(res).isNotNull();
    assertThat(res.getPermitido()).isFalse();
        assertThat(res.getMotivo()).isEqualTo("SIN_DERECHO_VIGENTE");
        assertThat(res.getOrientacionCaja()).isNotNull();
    }
}
