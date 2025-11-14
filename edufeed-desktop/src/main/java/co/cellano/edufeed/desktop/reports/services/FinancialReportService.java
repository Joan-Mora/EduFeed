package co.cellano.edufeed.desktop.reports.services;

import co.cellano.edufeed.desktop.reports.models.FinancialReportData;
import co.cellano.edufeed.desktop.reports.models.FinancialReportData.DailyRevenue;
import co.cellano.edufeed.desktop.reports.models.FinancialReportData.PaymentTypeDistribution;
import co.cellano.edufeed.desktop.reports.models.TransactionSummary;
import co.cellano.edufeed.desktop.service.PaymentApiClient;
import co.cellano.edufeed.desktop.service.PaymentApiClient.EstadoPago;
import co.cellano.edufeed.desktop.service.PaymentApiClient.PagoEnriquecidoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio completo para análisis financiero y generación de reportes
 * económicos.
 * Procesa transacciones, calcula métricas, rentabilidad, devoluciones, etc.
 */
public class FinancialReportService {

    private final PaymentApiClient paymentApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern MOTIVO_PATTERN = Pattern.compile("\\\"motivo\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern MOTIVO_DEVOLUCION_PATTERN = Pattern
            .compile("\\\"motivo_devolucion\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");

    public FinancialReportService(PaymentApiClient paymentApiClient) {
        this.paymentApiClient = paymentApiClient;
    }

    /**
     * Genera reporte financiero completo para el rango de fechas especificado.
     * Incluye: ingresos totales, devoluciones, rentabilidad, transacciones,
     * gráficas.
     */
    public FinancialReportData generateFinancialReport(LocalDate fechaDesde, LocalDate fechaHasta) {
        try {
            // Obtener todos los pagos del período
            List<PagoEnriquecidoDto> todosLosPagos = paymentApiClient.listarPagos();

            // Filtrar por rango de fechas
            List<PagoEnriquecidoDto> pagosFiltrados = todosLosPagos.stream()
                    .filter(p -> p.creadoEn != null)
                    .filter(p -> {
                        LocalDate fecha = p.creadoEn.toLocalDate();
                        boolean dentroRango = true;
                        if (fechaDesde != null && fecha.isBefore(fechaDesde))
                            dentroRango = false;
                        if (fechaHasta != null && fecha.isAfter(fechaHasta))
                            dentroRango = false;
                        return dentroRango;
                    })
                    .collect(Collectors.toList());

            return procesarDatosFinancieros(pagosFiltrados, fechaDesde, fechaHasta);

        } catch (Exception e) {
            // En caso de error, retornar reporte vacío
            FinancialReportData reporteVacio = new FinancialReportData();
            reporteVacio.setFechaDesde(fechaDesde);
            reporteVacio.setFechaHasta(fechaHasta);
            reporteVacio.setPeriodoDescripcion(generarDescripcionPeriodo(fechaDesde, fechaHasta));
            return reporteVacio;
        }
    }

    /**
     * Procesa todos los datos financieros y calcula métricas completas.
     */
    private FinancialReportData procesarDatosFinancieros(List<PagoEnriquecidoDto> pagos,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        FinancialReportData reporte = new FinancialReportData();
        reporte.setFechaDesde(fechaDesde);
        reporte.setFechaHasta(fechaHasta);
        reporte.setPeriodoDescripcion(generarDescripcionPeriodo(fechaDesde, fechaHasta));

        // Separar pagos por estado
        List<PagoEnriquecidoDto> aprobados = new ArrayList<>();
        List<PagoEnriquecidoDto> revertidos = new ArrayList<>();
        List<PagoEnriquecidoDto> rechazados = new ArrayList<>();

        for (PagoEnriquecidoDto pago : pagos) {
            if (pago.estadoPago == EstadoPago.APROBADO) {
                aprobados.add(pago);
            } else if (pago.estadoPago == EstadoPago.REVERTIDO) {
                revertidos.add(pago);
            } else if (pago.estadoPago == EstadoPago.RECHAZADO) {
                rechazados.add(pago);
            }
        }

        // Calcular ingresos totales (solo APROBADOS)
        BigDecimal ingresosTotales = aprobados.stream()
                .map(p -> p.monto != null ? p.monto : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular devoluciones (REVERTIDOS)
        BigDecimal devolucionesTotales = revertidos.stream()
                .map(p -> p.monto != null ? p.monto : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular ingresos netos
        BigDecimal ingresosNetos = ingresosTotales.subtract(devolucionesTotales);

        // Calcular porcentaje de rentabilidad
        BigDecimal porcentajeRentabilidad = calcularRentabilidad(ingresosTotales, devolucionesTotales);

        // Calcular ticket promedio (solo pagos aprobados)
        BigDecimal ticketPromedio = aprobados.isEmpty()
                ? BigDecimal.ZERO
                : ingresosTotales.divide(BigDecimal.valueOf(aprobados.size()), 2, RoundingMode.HALF_UP);

        // Establecer métricas en el reporte
        reporte.setIngresosTotales(ingresosTotales);
        reporte.setDevolucionesTotales(devolucionesTotales);
        reporte.setIngresosNetos(ingresosNetos);
        reporte.setPorcentajeRentabilidad(porcentajeRentabilidad);
        reporte.setTotalTransacciones((long) pagos.size());
        reporte.setTransaccionesAprobadas((long) aprobados.size());
        reporte.setTransaccionesRevertidas((long) revertidos.size());
        reporte.setTransaccionesRechazadas((long) rechazados.size());
        reporte.setTicketPromedio(ticketPromedio);

        // Generar datos para gráficas
        reporte.setIngresosDiarios(calcularIngresosDiarios(pagos, fechaDesde, fechaHasta));
        reporte.setDistribucionPorTipo(calcularDistribucionPorTipo(aprobados, ingresosTotales));

        // Generar resumen de transacciones
        reporte.setTransacciones(generarResumenTransacciones(pagos));

        return reporte;
    }

    /**
     * Calcula el porcentaje de rentabilidad.
     * Rentabilidad = ((Ingresos - Devoluciones) / Ingresos) * 100
     */
    private BigDecimal calcularRentabilidad(BigDecimal ingresos, BigDecimal devoluciones) {
        if (ingresos.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal neto = ingresos.subtract(devoluciones);
        return neto.divide(ingresos, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula ingresos y devoluciones agrupados por día.
     * Usado para gráfica de línea temporal.
     */
    private List<DailyRevenue> calcularIngresosDiarios(List<PagoEnriquecidoDto> pagos,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        // Asegurar que tenemos fechas válidas
        if (fechaDesde == null || fechaHasta == null) {
            if (pagos.isEmpty()) {
                fechaDesde = LocalDate.now().minusDays(30);
                fechaHasta = LocalDate.now();
            } else {
                fechaDesde = pagos.stream()
                        .map(p -> p.creadoEn.toLocalDate())
                        .min(LocalDate::compareTo)
                        .orElse(LocalDate.now().minusDays(30));
                fechaHasta = pagos.stream()
                        .map(p -> p.creadoEn.toLocalDate())
                        .max(LocalDate::compareTo)
                        .orElse(LocalDate.now());
            }
        }

        // Agrupar pagos por fecha
        Map<LocalDate, List<PagoEnriquecidoDto>> pagosPorDia = pagos.stream()
                .collect(Collectors.groupingBy(p -> p.creadoEn.toLocalDate()));

        // Generar lista de DailyRevenue para cada día del rango
        List<DailyRevenue> resultado = new ArrayList<>();
        LocalDate fechaActual = fechaDesde;

        while (!fechaActual.isAfter(fechaHasta)) {
            List<PagoEnriquecidoDto> pagosDia = pagosPorDia.getOrDefault(fechaActual, Collections.emptyList());

            BigDecimal ingresosDia = pagosDia.stream()
                    .filter(p -> p.estadoPago == EstadoPago.APROBADO)
                    .map(p -> p.monto != null ? p.monto : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal devolucionesDia = pagosDia.stream()
                    .filter(p -> p.estadoPago == EstadoPago.REVERTIDO)
                    .map(p -> p.monto != null ? p.monto : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            resultado.add(new DailyRevenue(fechaActual, ingresosDia, devolucionesDia));
            fechaActual = fechaActual.plusDays(1);
        }

        return resultado;
    }

    /**
     * Calcula la distribución de pagos por tipo.
     * Usado para gráfica de pastel/dona.
     */
    private List<PaymentTypeDistribution> calcularDistribucionPorTipo(List<PagoEnriquecidoDto> pagosAprobados,
            BigDecimal totalIngresos) {
        // Agrupar por tipo de pago
        Map<String, List<PagoEnriquecidoDto>> pagosPorTipo = pagosAprobados.stream()
                .collect(Collectors.groupingBy(p -> p.tipoPago != null ? p.tipoPago.name() : "DESCONOCIDO"));

        List<PaymentTypeDistribution> distribucion = new ArrayList<>();

        for (Map.Entry<String, List<PagoEnriquecidoDto>> entry : pagosPorTipo.entrySet()) {
            String tipo = entry.getKey();
            List<PagoEnriquecidoDto> pagos = entry.getValue();

            BigDecimal montoTotal = pagos.stream()
                    .map(p -> p.monto != null ? p.monto : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Long cantidad = (long) pagos.size();

            PaymentTypeDistribution dist = new PaymentTypeDistribution(tipo, montoTotal, cantidad);

            // Calcular porcentaje
            if (totalIngresos.compareTo(BigDecimal.ZERO) > 0) {
                double porcentaje = montoTotal.divide(totalIngresos, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                dist.setPorcentaje(porcentaje);
            } else {
                dist.setPorcentaje(0.0);
            }

            distribucion.add(dist);
        }

        // Ordenar por monto descendente
        distribucion.sort((a, b) -> b.getMonto().compareTo(a.getMonto()));

        return distribucion;
    }

    /**
     * Genera lista de TransactionSummary para tabla detallada.
     */
    private List<TransactionSummary> generarResumenTransacciones(List<PagoEnriquecidoDto> pagos) {
        List<TransactionSummary> transacciones = new ArrayList<>();

        for (PagoEnriquecidoDto pago : pagos) {
            TransactionSummary trans = new TransactionSummary(
                    pago.referenciaExterna,
                    pago.usuarioNombre,
                    pago.usuarioDocumento,
                    pago.tipoPago != null ? pago.tipoPago.name() : "N/A",
                    pago.metodoPago != null ? pago.metodoPago : "N/A",
                    pago.monto,
                    pago.estadoPago != null ? pago.estadoPago.name() : "N/A",
                    pago.creadoEn);

            // Extraer motivos si existen
            if (pago.metadatos != null && !pago.metadatos.isBlank()) {
                trans.setMotivo(extraerMotivoPago(pago.metadatos));
                trans.setMotivoDevolucion(extraerMotivoDevolucion(pago.metadatos));
            }

            transacciones.add(trans);
        }

        // Ordenar por fecha descendente (más recientes primero)
        transacciones.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));

        return transacciones;
    }

    /**
     * Genera descripción textual del período del reporte.
     */
    private String generarDescripcionPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return "Todos los registros";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (desde != null && hasta != null) {
            long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
            return String.format("%s - %s (%d días)",
                    desde.format(formatter),
                    hasta.format(formatter),
                    dias);
        } else if (desde != null) {
            return "Desde " + desde.format(formatter);
        } else {
            return "Hasta " + hasta.format(formatter);
        }
    }

    /**
     * Extrae el motivo del pago desde metadatos JSON.
     */
    private String extraerMotivoPago(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(metadatos);
            if (root.has("motivo")) {
                return limpiarTexto(root.get("motivo").asText());
            }
        } catch (Exception ignored) {
        }

        Matcher matcher = MOTIVO_PATTERN.matcher(metadatos);
        if (matcher.find()) {
            return limpiarTexto(matcher.group(1));
        }

        return null;
    }

    /**
     * Extrae el motivo de devolución desde metadatos JSON.
     */
    private String extraerMotivoDevolucion(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(metadatos);
            if (root.has("motivo_devolucion")) {
                return limpiarTexto(root.get("motivo_devolucion").asText());
            }
        } catch (Exception ignored) {
        }

        Matcher matcher = MOTIVO_DEVOLUCION_PATTERN.matcher(metadatos);
        if (matcher.find()) {
            return limpiarTexto(matcher.group(1));
        }

        return null;
    }

    /**
     * Limpia texto escapado de JSON.
     */
    private String limpiarTexto(String texto) {
        if (texto == null)
            return "";
        return texto.replace("\\n", " ")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }
}
