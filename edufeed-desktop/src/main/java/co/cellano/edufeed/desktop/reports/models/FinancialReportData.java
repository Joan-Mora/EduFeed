package co.cellano.edufeed.desktop.reports.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modelo de datos para el reporte financiero completo.
 * Contiene todas las métricas, transacciones y datos para visualización.
 */
public class FinancialReportData {
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    // Métricas principales
    private BigDecimal ingresosTotales;
    private BigDecimal devolucionesTotales;
    private BigDecimal ingresosNetos;
    private BigDecimal porcentajeRentabilidad;
    private Long totalTransacciones;
    private Long transaccionesAprobadas;
    private Long transaccionesRevertidas;
    private Long transaccionesRechazadas;

    // Datos detallados
    private List<TransactionSummary> transacciones;
    private List<DailyRevenue> ingresosDiarios;
    private List<PaymentTypeDistribution> distribucionPorTipo;

    // Métricas adicionales
    private BigDecimal ticketPromedio;
    private String periodoDescripcion;

    public FinancialReportData() {
        this.ingresosTotales = BigDecimal.ZERO;
        this.devolucionesTotales = BigDecimal.ZERO;
        this.ingresosNetos = BigDecimal.ZERO;
        this.porcentajeRentabilidad = BigDecimal.ZERO;
    }

    // Getters y Setters
    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public BigDecimal getIngresosTotales() {
        return ingresosTotales;
    }

    public void setIngresosTotales(BigDecimal ingresosTotales) {
        this.ingresosTotales = ingresosTotales;
    }

    public BigDecimal getDevolucionesTotales() {
        return devolucionesTotales;
    }

    public void setDevolucionesTotales(BigDecimal devolucionesTotales) {
        this.devolucionesTotales = devolucionesTotales;
    }

    public BigDecimal getIngresosNetos() {
        return ingresosNetos;
    }

    public void setIngresosNetos(BigDecimal ingresosNetos) {
        this.ingresosNetos = ingresosNetos;
    }

    public BigDecimal getPorcentajeRentabilidad() {
        return porcentajeRentabilidad;
    }

    public void setPorcentajeRentabilidad(BigDecimal porcentajeRentabilidad) {
        this.porcentajeRentabilidad = porcentajeRentabilidad;
    }

    public Long getTotalTransacciones() {
        return totalTransacciones;
    }

    public void setTotalTransacciones(Long totalTransacciones) {
        this.totalTransacciones = totalTransacciones;
    }

    public Long getTransaccionesAprobadas() {
        return transaccionesAprobadas;
    }

    public void setTransaccionesAprobadas(Long transaccionesAprobadas) {
        this.transaccionesAprobadas = transaccionesAprobadas;
    }

    public Long getTransaccionesRevertidas() {
        return transaccionesRevertidas;
    }

    public void setTransaccionesRevertidas(Long transaccionesRevertidas) {
        this.transaccionesRevertidas = transaccionesRevertidas;
    }

    public Long getTransaccionesRechazadas() {
        return transaccionesRechazadas;
    }

    public void setTransaccionesRechazadas(Long transaccionesRechazadas) {
        this.transaccionesRechazadas = transaccionesRechazadas;
    }

    public List<TransactionSummary> getTransacciones() {
        return transacciones;
    }

    public void setTransacciones(List<TransactionSummary> transacciones) {
        this.transacciones = transacciones;
    }

    public List<DailyRevenue> getIngresosDiarios() {
        return ingresosDiarios;
    }

    public void setIngresosDiarios(List<DailyRevenue> ingresosDiarios) {
        this.ingresosDiarios = ingresosDiarios;
    }

    public List<PaymentTypeDistribution> getDistribucionPorTipo() {
        return distribucionPorTipo;
    }

    public void setDistribucionPorTipo(List<PaymentTypeDistribution> distribucionPorTipo) {
        this.distribucionPorTipo = distribucionPorTipo;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }

    public String getPeriodoDescripcion() {
        return periodoDescripcion;
    }

    public void setPeriodoDescripcion(String periodoDescripcion) {
        this.periodoDescripcion = periodoDescripcion;
    }

    /**
     * Modelo para ingresos diarios (usado en gráfica de línea temporal)
     */
    public static class DailyRevenue {
        private LocalDate fecha;
        private BigDecimal ingresos;
        private BigDecimal devoluciones;
        private BigDecimal neto;

        public DailyRevenue(LocalDate fecha, BigDecimal ingresos, BigDecimal devoluciones) {
            this.fecha = fecha;
            this.ingresos = ingresos != null ? ingresos : BigDecimal.ZERO;
            this.devoluciones = devoluciones != null ? devoluciones : BigDecimal.ZERO;
            this.neto = this.ingresos.subtract(this.devoluciones);
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public BigDecimal getIngresos() {
            return ingresos;
        }

        public BigDecimal getDevoluciones() {
            return devoluciones;
        }

        public BigDecimal getNeto() {
            return neto;
        }
    }

    /**
     * Modelo para distribución por tipo de pago (usado en gráfica de pastel)
     */
    public static class PaymentTypeDistribution {
        private String tipoPago;
        private BigDecimal monto;
        private Long cantidad;
        private Double porcentaje;

        public PaymentTypeDistribution(String tipoPago, BigDecimal monto, Long cantidad) {
            this.tipoPago = tipoPago;
            this.monto = monto != null ? monto : BigDecimal.ZERO;
            this.cantidad = cantidad != null ? cantidad : 0L;
        }

        public String getTipoPago() {
            return tipoPago;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public Long getCantidad() {
            return cantidad;
        }

        public Double getPorcentaje() {
            return porcentaje;
        }

        public void setPorcentaje(Double porcentaje) {
            this.porcentaje = porcentaje;
        }
    }
}
