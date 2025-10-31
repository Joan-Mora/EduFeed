package co.cellano.edufeed.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.cellano.edufeed.backend.dto.request.WebhookPagoRequest;
import co.cellano.edufeed.backend.dto.response.TransaccionCajaResponse;
import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.TransaccionCaja;
import co.cellano.edufeed.backend.repository.PagoRepository;
import co.cellano.edufeed.backend.repository.TransaccionCajaRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransaccionCajaService - Webhook y listados")
class TransaccionCajaServiceTest {

    @Mock
    TransaccionCajaRepository transaccionCajaRepository;
    @Mock
    PagoRepository pagoRepository;

    @InjectMocks
    TransaccionCajaService service;

    @Test
    void procesarWebhook_conciliado() {
        WebhookPagoRequest req = new WebhookPagoRequest();
        req.setProveedor("WOMPI");
        req.setReferenciaExterna("ref-1");
        req.setMonto(new BigDecimal("10000"));
        req.setMetodoPago("QR");
        req.setEstado("APROBADO");
        req.setPayload("{}");

        Pago pago = new Pago();
        pago.setId(UUID.randomUUID());
        when(pagoRepository.findByReferenciaExterna("ref-1")).thenReturn(Optional.of(pago));

        when(transaccionCajaRepository.save(any(TransaccionCaja.class)))
                .thenAnswer(inv -> {
                    TransaccionCaja tx = inv.getArgument(0);
                    tx.setId(UUID.randomUUID());
                    return tx;
                });

        Map<String, Object> res = service.procesarWebhook(req);
        assertThat(res).containsEntry("recibido", true);
        assertThat(res).containsEntry("conciliado", true);
        assertThat(res.get("pagoId")).isNotNull();
    }

    @Test
    void listarTransacciones_paginado() {
        TransaccionCaja tx = new TransaccionCaja();
        tx.setId(UUID.randomUUID());
        tx.setProveedor("WOMPI");
        Page<TransaccionCaja> page = new PageImpl<>(List.of(tx));
        when(transaccionCajaRepository.findAllByOrderByRecibidoEnDesc(any(Pageable.class)))
                .thenReturn(page);

        Page<TransaccionCajaResponse> out = service.listarTransacciones(0, 10);
        assertThat(out.getContent()).hasSize(1);
        assertThat(out.getContent().get(0).getProveedor()).isEqualTo("WOMPI");
    }

    @Test
    void listarNoConciliadas_mapeoDesdeArray() {
        UUID id = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Object[] row = new Object[]{
                id, "WOMPI", "ref-9", new BigDecimal("1500"), "QR", "APROBADO",
                OffsetDateTime.now(), false, pagoId, "123", "Juan"
        };
    java.util.List<Object[]> rows = new java.util.ArrayList<>();
    rows.add(row);
    when(transaccionCajaRepository.findConDetallesByConciliado(false))
        .thenReturn(rows);

        List<TransaccionCajaResponse> out = service.listarNoConciliadas();
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo(id);
        assertThat(out.get(0).getUsuarioDocumento()).isEqualTo("123");
    }
}
