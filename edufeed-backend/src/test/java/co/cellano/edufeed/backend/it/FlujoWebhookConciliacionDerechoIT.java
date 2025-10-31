package co.cellano.edufeed.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.dto.request.AccesoCheckRequest;
import co.cellano.edufeed.backend.dto.request.WebhookPagoRequest;
import co.cellano.edufeed.backend.dto.response.AccesoCheckResponse;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import co.cellano.edufeed.backend.service.AccesoService;
import co.cellano.edufeed.backend.service.PagoService;
import co.cellano.edufeed.backend.service.TransaccionCajaService;
import co.cellano.edufeed.backend.service.UsuarioService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Flujo E2E: Webhook de caja → conciliación automática → aprobar pago → derecho activo / acceso permitido.
 * Nota: En la implementación actual, el webhook no aprueba pagos automáticamente; se simula la aprobación manual
 * posterior para completar el flujo hasta derecho activo.
 */
class FlujoWebhookConciliacionDerechoIT extends BaseIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private TransaccionCajaService transaccionCajaService;

    @Autowired
    private AccesoService accesoService;

    @Test
    @DisplayName("E2E: Webhook concilia por referencia y al aprobar el pago queda derecho activo")
    void webhookConciliacionDerechoActivo() {
        // Usuario
    UsuarioDto nuevo = new UsuarioDto();
    String doc = String.valueOf(ThreadLocalRandom.current().nextInt(900000000, 999999999));
    nuevo.setDocumento(doc);
        nuevo.setNombreCompleto("Estudiante Webhook");
        nuevo.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        UsuarioDto u = usuarioService.create(nuevo);
        UUID usuarioId = UUID.fromString(u.getId());

        // Crear pago pendiente con referencia externa
    String ref = "REF-IT-" + java.util.UUID.randomUUID().toString().substring(0,8);
        PagoDto p = new PagoDto();
        p.setUsuarioId(u.getId());
        p.setTipoPago(TipoPago.DIARIO);
        p.setMonto(new BigDecimal("15000"));
        p.setMetodoPago("PSE");
        p.setReferenciaExterna(ref);
        PagoDto creado = pagoService.create(p);

        // Recibir webhook con misma referencia (conciliación automática)
        WebhookPagoRequest w = new WebhookPagoRequest();
        w.setProveedor("CAJA-MOCK");
        w.setReferenciaExterna(ref);
        w.setMonto(new BigDecimal("15000"));
        w.setMetodoPago("PSE");
    w.setEstado("COMPLETADO");
    w.setPayload("{\"ok\":true}");
        Map<String,Object> result = transaccionCajaService.procesarWebhook(w);
        assertThat(result.get("recibido")).isEqualTo(true);
        assertThat(result.get("conciliado")).isEqualTo(true);
        assertThat(result.get("pagoId")).isNotNull();

        // Aprobar el pago para generar derecho
        pagoService.aprobar(UUID.fromString(creado.getId()));

        // Verificar acceso permitido
        AccesoCheckRequest req = new AccesoCheckRequest();
        req.setUsuarioId(usuarioId);
        req.setModalidad(Modalidad.HUELLA);
        AccesoCheckResponse res = accesoService.verificarAcceso(req);
        assertThat(res.getPermitido()).isTrue();
    }
}
