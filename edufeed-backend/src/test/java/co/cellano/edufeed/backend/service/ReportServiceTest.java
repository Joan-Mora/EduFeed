package co.cellano.edufeed.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.cellano.edufeed.backend.dto.response.AsistenciasDiariasItem;
import co.cellano.edufeed.backend.dto.response.DerechoActivoItem;
import co.cellano.edufeed.backend.dto.response.IngresosDiariosItem;
import co.cellano.edufeed.backend.dto.response.RechazosDiariosItem;
import co.cellano.edufeed.backend.repository.AccesoRepository;
import co.cellano.edufeed.backend.repository.DerechoUsoRepository;
import co.cellano.edufeed.backend.repository.PagoRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService - Agregaciones y mapeos")
class ReportServiceTest {

    @Mock
    PagoRepository pagoRepository;
    @Mock
    AccesoRepository accesoRepository;
    @Mock
    DerechoUsoRepository derechoUsoRepository;

    @InjectMocks
    ReportService reportService;

    @Test
    void ingresosDiarios_y_resumen() {
        OffsetDateTime d1 = OffsetDateTime.now().minusDays(1);
        OffsetDateTime d2 = OffsetDateTime.now();
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{Date.valueOf(d1.toLocalDate()), "DIARIO", "EFECTIVO", 2L, new BigDecimal("15000")});
        rows.add(new Object[]{Date.valueOf(d2.toLocalDate()), "MENSUAL", "QR", 1L, new BigDecimal("50000")});
        when(pagoRepository.aggregateIngresosDiarios(d1, d2)).thenReturn(rows);

        List<IngresosDiariosItem> out = reportService.ingresosDiarios(d1, d2);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).getMetodoPago()).isEqualTo("EFECTIVO");

        BigDecimal total = reportService.resumenIngresos(d1, d2);
        assertThat(total).isEqualByComparingTo(new BigDecimal("65000"));
    }

    @Test
    void asistencias_y_rechazos_diarios() {
        OffsetDateTime d1 = OffsetDateTime.now().minusDays(3);
        OffsetDateTime d2 = OffsetDateTime.now();
    java.util.List<Object[]> asistRows = new java.util.ArrayList<>();
    asistRows.add(new Object[]{Date.valueOf(d1.toLocalDate()), 10L, 8L});
    when(accesoRepository.aggregateAsistenciasDiarias(d1, d2)).thenReturn(asistRows);

        java.util.List<Object[]> rech = new java.util.ArrayList<>();
        rech.add(new Object[]{Date.valueOf(d2.toLocalDate()), "SIN_DERECHO", 3L});
        when(accesoRepository.aggregateRechazosDiarios(d1, d2)).thenReturn(rech);

        List<AsistenciasDiariasItem> asist = reportService.asistenciasDiarias(d1, d2);
        assertThat(asist).hasSize(1);
        assertThat(asist.get(0).getUsuariosUnicos()).isEqualTo(8L);

        List<RechazosDiariosItem> rej = reportService.rechazosDiarios(d1, d2);
        assertThat(rej).hasSize(1);
        assertThat(rej.get(0).getMotivoRechazo()).isEqualTo("SIN_DERECHO");
    }

    @Test
    void derechosActivos_mapeo() {
        OffsetDateTime now = OffsetDateTime.now();
        Timestamp desde = Timestamp.from(now.minusDays(1).toInstant());
        Timestamp hasta = Timestamp.from(now.plusDays(1).toInstant());
        java.util.List<Object[]> derechos = new java.util.ArrayList<>();
        derechos.add(new Object[]{"123", "Juan Perez", "DIARIO", desde, hasta, 5});
        when(derechoUsoRepository.findDerechosActivosConDetalle(any()))
                .thenReturn(derechos);

        List<DerechoActivoItem> out = reportService.derechosActivos();
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getUsuarioDocumento()).isEqualTo("123");
        assertThat(out.get(0).getDiasRestantes()).isEqualTo(5);
        assertThat(out.get(0).getVigenteDesde()).isNotNull();
        assertThat(out.get(0).getVigenteHasta()).isNotNull();
    }
}
