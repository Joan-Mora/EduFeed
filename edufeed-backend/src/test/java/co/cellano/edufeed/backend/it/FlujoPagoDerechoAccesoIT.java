package co.cellano.edufeed.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.dto.request.AccesoCheckRequest;
import co.cellano.edufeed.backend.dto.response.AccesoCheckResponse;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.service.AccesoService;
import co.cellano.edufeed.backend.service.PagoService;
import co.cellano.edufeed.backend.service.UsuarioService;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Flujo E2E: Crear pago → aprobar (genera derecho) → verificar acceso permitido.
 */
class FlujoPagoDerechoAccesoIT extends BaseIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private AccesoService accesoService;

    @Test
    @DisplayName("E2E: Pago DIARIO aprobado genera derecho y acceso permitido")
    void flujoPagoDiarioDerechoAcceso_permitido() {
        // Usuario
    UsuarioDto nuevo = new UsuarioDto();
    String doc = String.valueOf(ThreadLocalRandom.current().nextInt(900000000, 999999999));
    nuevo.setDocumento(doc);
        nuevo.setNombreCompleto("Estudiante Pago");
        nuevo.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        UsuarioDto u = usuarioService.create(nuevo);
        UUID usuarioId = UUID.fromString(u.getId());

        // Crear pago DIARIO pendiente
        PagoDto p = new PagoDto();
        p.setUsuarioId(u.getId());
        p.setTipoPago(TipoPago.DIARIO);
        p.setMonto(new BigDecimal("10000"));
        p.setMetodoPago("EFECTIVO");
        PagoDto creado = pagoService.create(p);

        // Aprobar pago (genera derecho)
        PagoDto aprobado = pagoService.aprobar(UUID.fromString(creado.getId()));
        assertThat(aprobado.getEstadoPago()).isNotNull();
        // Verificar acceso permitido
        AccesoCheckRequest req = new AccesoCheckRequest();
        req.setUsuarioId(usuarioId);
        req.setModalidad(Modalidad.HUELLA);
    AccesoCheckResponse res = accesoService.verificarAcceso(req);
    assertThat(res.getPermitido()).isTrue();
        assertThat(res.getDerecho()).isNotNull();
    }
}
