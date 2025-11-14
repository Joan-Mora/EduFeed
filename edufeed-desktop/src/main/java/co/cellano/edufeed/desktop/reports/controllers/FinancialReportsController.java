package co.cellano.edufeed.desktop.reports.controllers;

import co.cellano.edufeed.desktop.reports.models.FinancialReportData;
import co.cellano.edufeed.desktop.reports.services.CSVExportService;
import co.cellano.edufeed.desktop.reports.services.FinancialReportService;
import co.cellano.edufeed.desktop.reports.services.PDFExportService;
import co.cellano.edufeed.desktop.reports.views.FinancialDashboardView;
import co.cellano.edufeed.desktop.service.PaymentApiClient;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador para el dashboard de reportes financieros.
 * Maneja la lógica de negocio, carga de datos, cálculos y exportación.
 */
public class FinancialReportsController {

    private final FinancialDashboardView vista;
    private final FinancialReportService reporteService;
    private final PDFExportService pdfExportService;
    private final CSVExportService csvExportService;
    private final StackPane contenedorPrincipal;

    private ProgressIndicator loadingSpinner;

    /**
     * Constructor del controlador.
     * 
     * @param vista               Vista del dashboard
     * @param paymentApiClient    Cliente de API de pagos
     * @param contenedorPrincipal Contenedor para mostrar spinner de carga
     */
    public FinancialReportsController(FinancialDashboardView vista,
            PaymentApiClient paymentApiClient,
            StackPane contenedorPrincipal) {
        this.vista = vista;
        this.reporteService = new FinancialReportService(paymentApiClient);
        this.pdfExportService = new PDFExportService();
        this.csvExportService = new CSVExportService();
        this.contenedorPrincipal = contenedorPrincipal;

        inicializarLoadingSpinner();
        configurarCallbacks();
    }

    /**
     * Inicializa el spinner de carga.
     */
    private void inicializarLoadingSpinner() {
        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(80, 80);
        loadingSpinner.setStyle("-fx-progress-color: #007bff;");
        loadingSpinner.setVisible(false);
    }

    /**
     * Configura los callbacks de la vista.
     */
    private void configurarCallbacks() {
        vista.setOnActualizarFiltros(this::cargarDatosFinancieros);
        vista.setOnExportarPDF(this::exportarAPDF);
        vista.setOnExportarCSV(this::exportarACSV);
    }

    /**
     * Carga los datos iniciales al abrir el dashboard.
     */
    public void cargarDatosIniciales() {
        // Cargar último mes por defecto
        LocalDate hoy = LocalDate.now();
        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        cargarDatosFinancieros(primerDiaMes, hoy);
    }

    /**
     * Carga los datos financieros para el rango de fechas especificado.
     * Se ejecuta en background para no bloquear la UI.
     */
    private void cargarDatosFinancieros(LocalDate desde, LocalDate hasta) {
        // Validar fechas
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarError("Error de Fechas",
                    "La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'");
            return;
        }

        // Mostrar loading
        mostrarLoading(true);

        // Ejecutar en background
        CompletableFuture.supplyAsync(() -> {
            return reporteService.generateFinancialReport(desde, hasta);
        }).thenAcceptAsync(reporte -> {
            // Actualizar UI en JavaFX thread
            Platform.runLater(() -> {
                vista.actualizarVistaConDatos(reporte);
                mostrarLoading(false);
            });
        }, Platform::runLater).exceptionally(ex -> {
            Platform.runLater(() -> {
                mostrarLoading(false);
                mostrarError("Error al Cargar Datos",
                        "No se pudieron cargar los datos financieros: " + ex.getMessage());
            });
            return null;
        });
    }

    /**
     * Exporta el reporte actual a PDF.
     */
    private void exportarAPDF() {
        FinancialReportData reporte = vista.getReporteActual();

        if (reporte == null) {
            mostrarAdvertencia("Sin Datos",
                    "No hay datos para exportar. Por favor, cargue un reporte primero.");
            return;
        }

        // FileChooser para seleccionar ubicación
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte PDF");
        fileChooser.setInitialFileName(generarNombreArchivo(reporte, "pdf"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        File archivo = fileChooser.showSaveDialog(vista.getScene().getWindow());

        if (archivo != null) {
            mostrarLoading(true);

            CompletableFuture.runAsync(() -> {
                try {
                    // Capturar gráficas desde la vista
                    // Nota: Las gráficas deben capturarse en el thread de JavaFX
                    Platform.runLater(() -> {
                        try {
                            pdfExportService.exportarAPDF(
                                    reporte,
                                    archivo,
                                    null, // chartTemporal - capturado internamente
                                    null // chartDistribucion - capturado internamente
                            );

                            mostrarLoading(false);
                            mostrarExito("PDF Exportado",
                                    "El reporte se ha exportado correctamente a:\n" + archivo.getAbsolutePath());
                        } catch (Exception e) {
                            mostrarLoading(false);
                            mostrarError("Error al Exportar PDF",
                                    "No se pudo generar el PDF: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        mostrarLoading(false);
                        mostrarError("Error al Exportar PDF",
                                "No se pudo generar el PDF: " + e.getMessage());
                    });
                }
            });
        }
    }

    /**
     * Exporta el reporte actual a CSV.
     */
    private void exportarACSV() {
        FinancialReportData reporte = vista.getReporteActual();

        if (reporte == null) {
            mostrarAdvertencia("Sin Datos",
                    "No hay datos para exportar. Por favor, cargue un reporte primero.");
            return;
        }

        // FileChooser para seleccionar ubicación
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte CSV");
        fileChooser.setInitialFileName(generarNombreArchivo(reporte, "csv"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        File archivo = fileChooser.showSaveDialog(vista.getScene().getWindow());

        if (archivo != null) {
            mostrarLoading(true);

            CompletableFuture.runAsync(() -> {
                try {
                    csvExportService.exportarACSV(reporte, archivo);

                    Platform.runLater(() -> {
                        mostrarLoading(false);
                        mostrarExito("CSV Exportado",
                                "El reporte se ha exportado correctamente a:\n" + archivo.getAbsolutePath());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        mostrarLoading(false);
                        mostrarError("Error al Exportar CSV",
                                "No se pudo generar el CSV: " + e.getMessage());
                    });
                }
            });
        }
    }

    /**
     * Genera un nombre de archivo sugerido basado en el reporte.
     */
    private String generarNombreArchivo(FinancialReportData reporte, String extension) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fechaDesde = reporte.getFechaDesde() != null
                ? reporte.getFechaDesde().format(formatter)
                : "inicio";
        String fechaHasta = reporte.getFechaHasta() != null
                ? reporte.getFechaHasta().format(formatter)
                : "fin";

        return String.format("EduFeed_Reporte_Economico_%s_%s.%s",
                fechaDesde, fechaHasta, extension);
    }

    /**
     * Muestra u oculta el spinner de carga.
     */
    private void mostrarLoading(boolean mostrar) {
        if (contenedorPrincipal != null) {
            loadingSpinner.setVisible(mostrar);

            if (mostrar && !contenedorPrincipal.getChildren().contains(loadingSpinner)) {
                contenedorPrincipal.getChildren().add(loadingSpinner);
            } else if (!mostrar && contenedorPrincipal.getChildren().contains(loadingSpinner)) {
                contenedorPrincipal.getChildren().remove(loadingSpinner);
            }
        }
    }

    /**
     * Muestra un diálogo de error.
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de advertencia.
     */
    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de éxito.
     */
    private void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Libera recursos del controlador.
     */
    public void dispose() {
        // Limpiar recursos si es necesario
    }
}
