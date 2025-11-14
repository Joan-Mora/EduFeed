package co.cellano.edufeed.desktop.reports.services;

import co.cellano.edufeed.desktop.reports.models.FinancialReportData;
import co.cellano.edufeed.desktop.reports.models.TransactionSummary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para exportación de reportes financieros a CSV.
 * Genera archivos CSV compatibles con Excel y bases de datos:
 * - Encoding UTF-8 con BOM para compatibilidad con Excel
 * - Delimitador punto y coma (;) estándar español
 * - Headers descriptivos en español
 * - Formato de números con punto decimal
 * - Sección de resumen ejecutivo al inicio
 */
public class CSVExportService {

    private static final String DELIMITER = ";";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // BOM (Byte Order Mark) para UTF-8 - necesario para que Excel reconozca UTF-8
    private static final byte[] UTF8_BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /**
     * Exporta el reporte financiero a un archivo CSV compatible con Excel.
     * Incluye sección de resumen ejecutivo y tabla detallada de transacciones.
     * 
     * @param reporte        Datos del reporte financiero
     * @param archivoDestino Archivo CSV de destino
     * @throws IOException Si hay error al generar el CSV
     */
    public void exportarACSV(FinancialReportData reporte, File archivoDestino) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(archivoDestino);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

            // Escribir BOM para que Excel detecte UTF-8
            fos.write(UTF8_BOM);

            // Sección 1: Información del reporte
            escribirInfoReporte(writer, reporte);

            // Sección 2: Resumen ejecutivo (métricas clave)
            escribirResumenEjecutivo(writer, reporte);

            // Sección 3: Tabla detallada de transacciones
            escribirTablaTransacciones(writer, reporte);

            writer.flush();
        }
    }

    /**
     * Escribe la sección de información general del reporte.
     */
    private void escribirInfoReporte(BufferedWriter writer, FinancialReportData reporte) throws IOException {
        writer.write("=== REPORTE ECONÓMICO EDUFEED ===" + LINE_SEPARATOR);
        writer.write("Período" + DELIMITER + escaparCSV(reporte.getPeriodoDescripcion()) + LINE_SEPARATOR);
        writer.write("Fecha Desde" + DELIMITER +
                (reporte.getFechaDesde() != null ? reporte.getFechaDesde().toString() : "N/A") + LINE_SEPARATOR);
        writer.write("Fecha Hasta" + DELIMITER +
                (reporte.getFechaHasta() != null ? reporte.getFechaHasta().toString() : "N/A") + LINE_SEPARATOR);
        writer.write(LINE_SEPARATOR);
    }

    /**
     * Escribe la sección de resumen ejecutivo con métricas principales.
     */
    private void escribirResumenEjecutivo(BufferedWriter writer, FinancialReportData reporte) throws IOException {
        writer.write("=== RESUMEN EJECUTIVO ===" + LINE_SEPARATOR);
        writer.write("Métrica" + DELIMITER + "Valor" + LINE_SEPARATOR);

        // Métricas principales
        writer.write("Ingresos Totales" + DELIMITER + formatearMoneda(reporte.getIngresosTotales()) + LINE_SEPARATOR);
        writer.write("Devoluciones Totales" + DELIMITER + formatearMoneda(reporte.getDevolucionesTotales())
                + LINE_SEPARATOR);
        writer.write("Ingresos Netos" + DELIMITER + formatearMoneda(reporte.getIngresosNetos()) + LINE_SEPARATOR);
        writer.write("Porcentaje Rentabilidad" + DELIMITER +
                formatearPorcentaje(reporte.getPorcentajeRentabilidad()) + LINE_SEPARATOR);
        writer.write("Ticket Promedio" + DELIMITER +
                formatearMoneda(reporte.getTicketPromedio() != null ? reporte.getTicketPromedio() : BigDecimal.ZERO)
                + LINE_SEPARATOR);

        writer.write(LINE_SEPARATOR);

        // Métricas de transacciones
        writer.write("Total Transacciones" + DELIMITER + reporte.getTotalTransacciones() + LINE_SEPARATOR);
        writer.write("Transacciones Aprobadas" + DELIMITER + reporte.getTransaccionesAprobadas() + LINE_SEPARATOR);
        writer.write("Transacciones Revertidas" + DELIMITER + reporte.getTransaccionesRevertidas() + LINE_SEPARATOR);
        writer.write("Transacciones Rechazadas" + DELIMITER + reporte.getTransaccionesRechazadas() + LINE_SEPARATOR);

        writer.write(LINE_SEPARATOR);
        writer.write(LINE_SEPARATOR);
    }

    /**
     * Escribe la tabla detallada de transacciones.
     */
    private void escribirTablaTransacciones(BufferedWriter writer, FinancialReportData reporte) throws IOException {
        writer.write("=== DETALLE DE TRANSACCIONES ===" + LINE_SEPARATOR);

        // Headers
        writer.write("Fecha" + DELIMITER);
        writer.write("Referencia" + DELIMITER);
        writer.write("Usuario" + DELIMITER);
        writer.write("Documento" + DELIMITER);
        writer.write("Tipo Pago" + DELIMITER);
        writer.write("Método Pago" + DELIMITER);
        writer.write("Monto" + DELIMITER);
        writer.write("Estado" + DELIMITER);
        writer.write("Motivo Pago" + DELIMITER);
        writer.write("Motivo Devolución" + LINE_SEPARATOR);

        // Filas de datos
        if (reporte.getTransacciones() != null) {
            for (TransactionSummary trans : reporte.getTransacciones()) {
                writer.write(escaparCSV(trans.getFecha() != null ? trans.getFecha().format(DATETIME_FORMAT) : "N/A")
                        + DELIMITER);
                writer.write(escaparCSV(trans.getReferenciaExterna()) + DELIMITER);
                writer.write(escaparCSV(trans.getUsuarioNombre()) + DELIMITER);
                writer.write(escaparCSV(trans.getUsuarioDocumento()) + DELIMITER);
                writer.write(escaparCSV(trans.getTipoPago()) + DELIMITER);
                writer.write(escaparCSV(trans.getMetodoPago()) + DELIMITER);
                writer.write(formatearMoneda(trans.getMonto()) + DELIMITER);
                writer.write(escaparCSV(trans.getEstadoPago()) + DELIMITER);
                writer.write(escaparCSV(trans.getMotivo()) + DELIMITER);
                writer.write(escaparCSV(trans.getMotivoDevolucion()) + LINE_SEPARATOR);
            }
        }
    }

    /**
     * Escapa valores CSV según RFC 4180.
     * Si el valor contiene delimitador, comillas o saltos de línea, se encierra
     * entre comillas
     * y las comillas internas se duplican.
     */
    private String escaparCSV(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }

        // Si contiene delimitador, comillas, o saltos de línea, envolver entre comillas
        if (valor.contains(DELIMITER) || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            // Duplicar comillas internas
            String escapado = valor.replace("\"", "\"\"");
            return "\"" + escapado + "\"";
        }

        return valor;
    }

    /**
     * Formatea un BigDecimal como moneda en formato numérico (sin símbolo).
     * Usa punto como separador decimal para compatibilidad con Excel.
     */
    private String formatearMoneda(BigDecimal monto) {
        if (monto == null) {
            return "0.00";
        }
        // Formato: 1234567.89 (sin separador de miles, punto decimal)
        return String.format("%.2f", monto.doubleValue());
    }

    /**
     * Formatea un porcentaje con 2 decimales.
     */
    private String formatearPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null) {
            return "0.00%";
        }
        return String.format("%.2f%%", porcentaje.doubleValue());
    }
}
