package co.cellano.edufeed.desktop.reports.services;

import co.cellano.edufeed.desktop.reports.models.FinancialReportData;
import co.cellano.edufeed.desktop.reports.models.TransactionSummary;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para exportación de reportes financieros a PDF.
 * Genera PDFs profesionales con:
 * - Logo y marca de agua EduFeed
 * - Tablas estructuradas con colores corporativos
 * - Gráficas embebidas
 * - Headers y footers con copyright
 */
public class PDFExportService {

    private static final Color COLOR_PRIMARY = new DeviceRgb(0, 123, 255); // #007bff
    private static final Color COLOR_SUCCESS = new DeviceRgb(40, 167, 69); // #28a745
    private static final Color COLOR_DANGER = new DeviceRgb(220, 53, 69); // #dc3545
    private static final Color COLOR_WARNING = new DeviceRgb(255, 193, 7); // #ffc107
    private static final Color COLOR_GRAY = new DeviceRgb(108, 117, 125); // #6c757d
    private static final Color COLOR_LIGHT_GRAY = new DeviceRgb(248, 249, 250); // #f8f9fa

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Exporta el reporte financiero a un archivo PDF.
     * 
     * @param reporte           Datos del reporte financiero
     * @param archivoDestino    Archivo PDF de destino
     * @param chartTemporal     Gráfica temporal (opcional)
     * @param chartDistribucion Gráfica de distribución (opcional)
     * @throws IOException Si hay error al generar el PDF
     */
    public void exportarAPDF(FinancialReportData reporte,
            File archivoDestino,
            LineChart<?, ?> chartTemporal,
            PieChart chartDistribucion) throws IOException {

        PdfWriter writer = new PdfWriter(archivoDestino);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        try {
            // Agregar marca de agua en cada página
            agregarMarcaDeAgua(pdfDoc);

            // Página 1: Portada y resumen ejecutivo
            agregarPortada(document, reporte);
            agregarResumenEjecutivo(document, reporte);

            // Página 2: Gráficas (si están disponibles)
            if (chartTemporal != null || chartDistribucion != null) {
                document.add(new AreaBreak());
                agregarGraficas(document, chartTemporal, chartDistribucion);
            }

            // Página 3+: Tabla detallada de transacciones
            if (reporte.getTransacciones() != null && !reporte.getTransacciones().isEmpty()) {
                document.add(new AreaBreak());
                agregarTablaTransacciones(document, reporte);
            }

            // Footer en todas las páginas
            agregarFooter(pdfDoc, document);

        } finally {
            document.close();
        }
    }

    /**
     * Agrega marca de agua "EduFeed" semi-transparente en todas las páginas.
     */
    private void agregarMarcaDeAgua(PdfDocument pdfDoc) {
        int numPaginas = pdfDoc.getNumberOfPages();

        for (int i = 1; i <= numPaginas; i++) {
            try {
                PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(i).newContentStreamBefore(),
                        pdfDoc.getPage(i).getResources(),
                        pdfDoc);

                PdfExtGState gs1 = new PdfExtGState();
                gs1.setFillOpacity(0.05f);
                canvas.saveState();
                canvas.setExtGState(gs1);

                // Dibujar texto "EduFeed" en diagonal
                canvas.beginText()
                        .setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(
                                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD), 80)
                        .setColor(COLOR_PRIMARY, true)
                        .moveText(150, 400)
                        .showText("EduFeed")
                        .endText();

                canvas.restoreState();
            } catch (IOException e) {
                System.err.println("[PDFExportService] Error agregando marca de agua: " + e.getMessage());
            }
        }
    }

    /**
     * Agrega portada con logo y título del reporte.
     */
    private void agregarPortada(Document document, FinancialReportData reporte) {
        // Título principal
        Paragraph titulo = new Paragraph("EduFeed")
                .setFontSize(42)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_PRIMARY)
                .setMarginBottom(10);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Reporte Económico y Financiero")
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_GRAY)
                .setMarginBottom(30);
        document.add(subtitulo);

        // Información del período
        Paragraph periodo = new Paragraph("Período: " + reporte.getPeriodoDescripcion())
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_GRAY)
                .setMarginBottom(20);
        document.add(periodo);

        // Fecha de generación
        Paragraph fechaGen = new Paragraph("Generado: " + LocalDateTime.now().format(DATETIME_FORMAT))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_GRAY)
                .setMarginBottom(40);
        document.add(fechaGen);

        // Línea separadora
        document.add(new Paragraph("\n"));
    }

    /**
     * Agrega resumen ejecutivo con métricas clave en formato de cards.
     */
    private void agregarResumenEjecutivo(Document document, FinancialReportData reporte) {
        // Título de sección
        Paragraph tituloSeccion = new Paragraph("📊 Resumen Ejecutivo")
                .setFontSize(20)
                .setBold()
                .setFontColor(COLOR_PRIMARY)
                .setMarginBottom(20);
        document.add(tituloSeccion);

        // Tabla 2x4 para métricas principales
        Table tablaMetricas = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 25, 25 }))
                .useAllAvailableWidth()
                .setMarginBottom(30);

        // Fila 1: Ingresos, Devoluciones, Netos, Rentabilidad
        tablaMetricas.addCell(crearCeldaMetrica("💵 Ingresos Totales",
                formatearMoneda(reporte.getIngresosTotales()), COLOR_SUCCESS));
        tablaMetricas.addCell(crearCeldaMetrica("🔄 Devoluciones",
                formatearMoneda(reporte.getDevolucionesTotales()), COLOR_DANGER));
        tablaMetricas.addCell(crearCeldaMetrica("💰 Ingresos Netos",
                formatearMoneda(reporte.getIngresosNetos()), COLOR_PRIMARY));
        tablaMetricas.addCell(crearCeldaMetrica("📈 Rentabilidad",
                String.format("%.2f%%", reporte.getPorcentajeRentabilidad()), COLOR_WARNING));

        // Fila 2: Transacciones
        tablaMetricas.addCell(crearCeldaMetricaSecundaria("Total Transacciones",
                String.valueOf(reporte.getTotalTransacciones())));
        tablaMetricas.addCell(crearCeldaMetricaSecundaria("Aprobadas",
                String.valueOf(reporte.getTransaccionesAprobadas())));
        tablaMetricas.addCell(crearCeldaMetricaSecundaria("Revertidas",
                String.valueOf(reporte.getTransaccionesRevertidas())));
        tablaMetricas.addCell(crearCeldaMetricaSecundaria("Ticket Promedio",
                formatearMoneda(reporte.getTicketPromedio())));

        document.add(tablaMetricas);
    }

    /**
     * Crea celda de métrica principal con estilo destacado.
     */
    private Cell crearCeldaMetrica(String titulo, String valor, Color color) {
        Paragraph pTitulo = new Paragraph(titulo)
                .setFontSize(10)
                .setFontColor(COLOR_GRAY)
                .setMarginBottom(5);

        Paragraph pValor = new Paragraph(valor)
                .setFontSize(18)
                .setBold()
                .setFontColor(color);

        return new Cell()
                .add(pTitulo)
                .add(pValor)
                .setBackgroundColor(COLOR_LIGHT_GRAY)
                .setPadding(15)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(color, 2));
    }

    /**
     * Crea celda de métrica secundaria con estilo simple.
     */
    private Cell crearCeldaMetricaSecundaria(String titulo, String valor) {
        Paragraph pTitulo = new Paragraph(titulo)
                .setFontSize(9)
                .setFontColor(COLOR_GRAY)
                .setMarginBottom(3);

        Paragraph pValor = new Paragraph(valor)
                .setFontSize(14)
                .setBold()
                .setFontColor(COLOR_GRAY);

        return new Cell()
                .add(pTitulo)
                .add(pValor)
                .setBackgroundColor(new DeviceRgb(255, 255, 255))
                .setPadding(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(new DeviceRgb(222, 226, 230), 1));
    }

    /**
     * Agrega gráficas al PDF (capturadas como imágenes).
     */
    private void agregarGraficas(Document document, LineChart<?, ?> chartTemporal, PieChart chartDistribucion)
            throws IOException {

        Paragraph tituloSeccion = new Paragraph("📈 Análisis Gráfico")
                .setFontSize(20)
                .setBold()
                .setFontColor(COLOR_PRIMARY)
                .setMarginBottom(20);
        document.add(tituloSeccion);

        if (chartTemporal != null) {
            Paragraph tituloGrafica = new Paragraph("Evolución Temporal de Ingresos")
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(10);
            document.add(tituloGrafica);

            byte[] imagenBytes = capturarGraficaComoImagen(chartTemporal);
            if (imagenBytes != null) {
                Image imagen = new Image(ImageDataFactory.create(imagenBytes));
                imagen.setWidth(UnitValue.createPercentValue(90));
                imagen.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                document.add(imagen);
                document.add(new Paragraph("\n"));
            }
        }

        if (chartDistribucion != null) {
            Paragraph tituloGrafica = new Paragraph("Distribución por Tipo de Pago")
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(10);
            document.add(tituloGrafica);

            byte[] imagenBytes = capturarGraficaComoImagen(chartDistribucion);
            if (imagenBytes != null) {
                Image imagen = new Image(ImageDataFactory.create(imagenBytes));
                imagen.setWidth(UnitValue.createPercentValue(70));
                imagen.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                document.add(imagen);
            }
        }
    }

    /**
     * Captura una gráfica JavaFX como imagen PNG.
     */
    private byte[] capturarGraficaComoImagen(javafx.scene.chart.Chart chart) throws IOException {
        try {
            WritableImage snapshot = chart.snapshot(new SnapshotParameters(), null);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("[PDFExportService] Error capturando gráfica: " + e.getMessage());
            return null;
        }
    }

    /**
     * Agrega tabla detallada de transacciones.
     */
    private void agregarTablaTransacciones(Document document, FinancialReportData reporte) {
        Paragraph tituloSeccion = new Paragraph("📋 Detalle de Transacciones")
                .setFontSize(20)
                .setBold()
                .setFontColor(COLOR_PRIMARY)
                .setMarginBottom(20);
        document.add(tituloSeccion);

        // Crear tabla con 8 columnas
        float[] columnWidths = { 12, 15, 20, 12, 10, 12, 12, 10 };
        Table tabla = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        // Headers
        tabla.addHeaderCell(crearCeldaHeader("Fecha"));
        tabla.addHeaderCell(crearCeldaHeader("Referencia"));
        tabla.addHeaderCell(crearCeldaHeader("Usuario"));
        tabla.addHeaderCell(crearCeldaHeader("Documento"));
        tabla.addHeaderCell(crearCeldaHeader("Tipo"));
        tabla.addHeaderCell(crearCeldaHeader("Método"));
        tabla.addHeaderCell(crearCeldaHeader("Monto"));
        tabla.addHeaderCell(crearCeldaHeader("Estado"));

        // Filas de datos
        for (TransactionSummary trans : reporte.getTransacciones()) {
            tabla.addCell(crearCeldaDato(trans.getFecha().format(DATE_FORMAT)));
            tabla.addCell(crearCeldaDato(trans.getReferenciaExterna() != null ? trans.getReferenciaExterna() : "N/A"));
            tabla.addCell(crearCeldaDato(trans.getUsuarioNombre() != null ? trans.getUsuarioNombre() : "N/A"));
            tabla.addCell(crearCeldaDato(trans.getUsuarioDocumento() != null ? trans.getUsuarioDocumento() : "N/A"));
            tabla.addCell(crearCeldaDato(trans.getTipoPago()));
            tabla.addCell(crearCeldaDato(trans.getMetodoPago()));
            tabla.addCell(crearCeldaDato(formatearMoneda(trans.getMonto())));
            tabla.addCell(crearCeldaEstado(trans.getEstadoPago()));
        }

        document.add(tabla);
    }

    /**
     * Crea celda de header para tabla.
     */
    private Cell crearCeldaHeader(String texto) {
        return new Cell()
                .add(new Paragraph(texto).setFontSize(9).setBold().setFontColor(new DeviceRgb(255, 255, 255)))
                .setBackgroundColor(COLOR_PRIMARY)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    /**
     * Crea celda de dato para tabla.
     */
    private Cell crearCeldaDato(String texto) {
        return new Cell()
                .add(new Paragraph(texto).setFontSize(8))
                .setPadding(6)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(new DeviceRgb(222, 226, 230), 0.5f));
    }

    /**
     * Crea celda de estado con color según el valor.
     */
    private Cell crearCeldaEstado(String estado) {
        Color color = switch (estado) {
            case "APROBADO" -> COLOR_SUCCESS;
            case "REVERTIDO" -> new DeviceRgb(253, 126, 20); // #fd7e14
            case "RECHAZADO" -> COLOR_DANGER;
            case "PENDIENTE" -> COLOR_WARNING;
            default -> COLOR_GRAY;
        };

        return new Cell()
                .add(new Paragraph(estado).setFontSize(8).setBold().setFontColor(color))
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(new DeviceRgb(222, 226, 230), 0.5f));
    }

    /**
     * Agrega footer con copyright en todas las páginas.
     */
    private void agregarFooter(PdfDocument pdfDoc, Document document) {
        int numPaginas = pdfDoc.getNumberOfPages();

        for (int i = 1; i <= numPaginas; i++) {
            Paragraph footer = new Paragraph(
                    String.format("© %d EduFeed - Sistema de Control de Acceso y Pagos | Página %d de %d",
                            LocalDateTime.now().getYear(), i, numPaginas))
                    .setFontSize(8)
                    .setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);

            document.showTextAligned(footer,
                    pdfDoc.getPage(i).getPageSize().getWidth() / 2,
                    20,
                    i,
                    TextAlignment.CENTER,
                    VerticalAlignment.BOTTOM,
                    0);
        }
    }

    /**
     * Formatea un BigDecimal como moneda en pesos colombianos.
     */
    private String formatearMoneda(BigDecimal monto) {
        if (monto == null) {
            return "$0";
        }
        return "$" + String.format("%,.0f", monto.doubleValue());
    }
}
