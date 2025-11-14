package co.cellano.edufeed.desktop.reports.views;

import co.cellano.edufeed.desktop.reports.models.FinancialReportData;
import co.cellano.edufeed.desktop.reports.models.FinancialReportData.DailyRevenue;
import co.cellano.edufeed.desktop.reports.models.FinancialReportData.PaymentTypeDistribution;
import co.cellano.edufeed.desktop.reports.models.TransactionSummary;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Vista principal del Dashboard Financiero.
 * Muestra métricas, gráficas y tabla detallada de transacciones económicas.
 * Diseño profesional con cards de métricas, charts y exportación PDF/CSV.
 */
public class FinancialDashboardView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Controles de filtros
    private final DatePicker filtroDesde = new DatePicker();
    private final DatePicker filtroHasta = new DatePicker();
    private final Button btnHoy = new Button("Hoy");
    private final Button btnSemana = new Button("Semana");
    private final Button btnMes = new Button("Mes");
    private final Button btnAnio = new Button("Año");
    private final Button btnActualizar = new Button("\uD83D\uDD04 Actualizar"); // 🔄

    // Botones de exportación
    private final Button btnExportPDF = new Button("\uD83D\uDCC4 Exportar PDF"); // 📄
    private final Button btnExportCSV = new Button("\uD83D\uDCCA Exportar CSV"); // 📊

    // Cards de métricas
    private final Label lblIngresosTotales = new Label("$0");
    private final Label lblDevolucionesTotales = new Label("$0");
    private final Label lblIngresosNetos = new Label("$0");
    private final Label lblRentabilidad = new Label("0%");
    private final Label lblTransaccionesTotal = new Label("0");
    private final Label lblTransaccionesAprobadas = new Label("0");
    private final Label lblTransaccionesRevertidas = new Label("0");
    private final Label lblTicketPromedio = new Label("$0");

    // Gráficas
    private final LineChart<String, Number> chartTemporal;
    private final BarChart<String, Number> chartComparativo;
    private final PieChart chartDistribucion;

    // Tabla de transacciones
    private final TableView<TransactionSummary> tableTransacciones = new TableView<>();
    private final Label lblPeriodo = new Label("");

    // Callbacks
    private BiConsumer<LocalDate, LocalDate> onActualizarFiltros;
    private Runnable onExportarPDF;
    private Runnable onExportarCSV;

    // Datos actuales
    private FinancialReportData reporteActual;

    public FinancialDashboardView() {
        setSpacing(20);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f8f9fa 0%, #e9ecef 100%);");

        // Inicializar gráficas
        chartTemporal = crearChartTemporal();
        chartComparativo = crearChartComparativo();
        chartDistribucion = crearChartDistribucion();

        construirUI();
        configurarEventos();
        establecerFechasPorDefecto();
    }

    private void construirUI() {
        // Header con título y período
        VBox header = construirHeader();

        // Panel de filtros y exportación
        HBox panelFiltros = construirPanelFiltros();

        // Grid de cards de métricas (4 columnas)
        GridPane gridMetricas = construirGridMetricas();

        // Panel de gráficas (3 gráficas en grid)
        GridPane gridGraficas = construirGridGraficas();

        // Tabla de transacciones
        VBox panelTabla = construirPanelTabla();

        getChildren().addAll(header, panelFiltros, gridMetricas, gridGraficas, panelTabla);
    }

    private VBox construirHeader() {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        // Unicode escape para emoji de bolsa de dinero
        Label titulo = new Label("\uD83D\uDCB0 Reportes Económicos"); // 💰
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titulo.setStyle("-fx-text-fill: #212529;");

        lblPeriodo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        lblPeriodo.setStyle("-fx-text-fill: #6c757d;");

        header.getChildren().addAll(titulo, lblPeriodo);
        return header;
    }

    private HBox construirPanelFiltros() {
        HBox panel = new HBox(15);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 2);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;");

        // Sección de fechas
        VBox seccionFechas = new VBox(8);
        Label lblFechas = new Label("\uD83D\uDCC5 Rango de Fechas"); // 📅
        lblFechas.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblFechas.setStyle("-fx-text-fill: #495057;");

        HBox boxFechas = new HBox(10);
        boxFechas.setAlignment(Pos.CENTER_LEFT);

        Label lblDesde = new Label("Desde:");
        lblDesde.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
        filtroDesde.setPrefWidth(150);
        estilizarDatePicker(filtroDesde);

        Label lblHasta = new Label("Hasta:");
        lblHasta.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
        filtroHasta.setPrefWidth(150);
        estilizarDatePicker(filtroHasta);

        boxFechas.getChildren().addAll(lblDesde, filtroDesde, lblHasta, filtroHasta);
        seccionFechas.getChildren().addAll(lblFechas, boxFechas);

        // Separador vertical
        Region separador1 = new Region();
        separador1.setPrefWidth(2);
        separador1.setStyle("-fx-background-color: #dee2e6;");

        // Botones rápidos
        VBox seccionRapidos = new VBox(8);
        Label lblRapidos = new Label("⚡ Acceso Rápido");
        lblRapidos.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblRapidos.setStyle("-fx-text-fill: #495057;");

        HBox boxRapidos = new HBox(8);
        boxRapidos.setAlignment(Pos.CENTER_LEFT);
        estilizarBotonRapido(btnHoy);
        estilizarBotonRapido(btnSemana);
        estilizarBotonRapido(btnMes);
        estilizarBotonRapido(btnAnio);
        boxRapidos.getChildren().addAll(btnHoy, btnSemana, btnMes, btnAnio);

        seccionRapidos.getChildren().addAll(lblRapidos, boxRapidos);

        // Separador vertical
        Region separador2 = new Region();
        separador2.setPrefWidth(2);
        separador2.setStyle("-fx-background-color: #dee2e6;");

        // Botón actualizar
        VBox seccionAcciones = new VBox(8);
        seccionAcciones.setAlignment(Pos.CENTER);
        btnActualizar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnActualizar.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #007bff 0%, #0056b3 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 12 24;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.4), 8, 0, 0, 2);");

        // Botones de exportación
        HBox boxExportar = new HBox(8);
        boxExportar.setAlignment(Pos.CENTER);
        estilizarBotonExportar(btnExportPDF, "#dc3545", "#c82333");
        estilizarBotonExportar(btnExportCSV, "#28a745", "#218838");
        boxExportar.getChildren().addAll(btnExportPDF, btnExportCSV);

        seccionAcciones.getChildren().addAll(btnActualizar, boxExportar);

        panel.getChildren().addAll(seccionFechas, separador1, seccionRapidos, separador2, seccionAcciones);
        return panel;
    }

    private GridPane construirGridMetricas() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Configurar columnas para que sean iguales
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        // Fila 1: Ingresos, Devoluciones, Neto, Rentabilidad
        VBox cardIngresos = crearCardMetrica("\uD83D\uDCB5 Ingresos Totales", lblIngresosTotales, "#28a745"); // 💵
        VBox cardDevoluciones = crearCardMetrica("\uD83D\uDD04 Devoluciones", lblDevolucionesTotales, "#dc3545"); // 🔄
        VBox cardNetos = crearCardMetrica("\uD83D\uDCB0 Ingresos Netos", lblIngresosNetos, "#007bff"); // 💰
        VBox cardRentabilidad = crearCardMetrica("\uD83D\uDCC8 Rentabilidad", lblRentabilidad, "#ffc107"); // 📈

        grid.add(cardIngresos, 0, 0);
        grid.add(cardDevoluciones, 1, 0);
        grid.add(cardNetos, 2, 0);
        grid.add(cardRentabilidad, 3, 0);

        // Fila 2: Total transacciones, Aprobadas, Revertidas, Ticket promedio
        VBox cardTotal = crearCardMetricaSecundaria("\uD83D\uDCCA Total Transacciones", lblTransaccionesTotal,
                "#6c757d"); // 📊
        VBox cardAprobadas = crearCardMetricaSecundaria("\u2705 Aprobadas", lblTransaccionesAprobadas, "#28a745"); // ✅
        VBox cardRevertidas = crearCardMetricaSecundaria("\u21A9\uFE0F Revertidas", lblTransaccionesRevertidas,
                "#fd7e14"); // ↩️
        VBox cardTicket = crearCardMetricaSecundaria("\uD83C\uDFAB Ticket Promedio", lblTicketPromedio, "#17a2b8"); // 🎫

        grid.add(cardTotal, 0, 1);
        grid.add(cardAprobadas, 1, 1);
        grid.add(cardRevertidas, 2, 1);
        grid.add(cardTicket, 3, 1);

        return grid;
    }

    private VBox crearCardMetrica(String titulo, Label valorLabel, String color) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(24));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 15, 0, 0, 3);" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 4 0;" +
                        "-fx-border-radius: 16;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lblTitulo.setStyle("-fx-text-fill: #6c757d;");
        lblTitulo.setWrapText(true);

        valorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        valorLabel.setStyle("-fx-text-fill: " + color + ";");
        valorLabel.setWrapText(true);

        card.getChildren().addAll(lblTitulo, valorLabel);

        // Animación hover
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return card;
    }

    private VBox crearCardMetricaSecundaria(String titulo, Label valorLabel, String color) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lblTitulo.setStyle("-fx-text-fill: #6c757d;");
        lblTitulo.setWrapText(true);

        valorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        valorLabel.setStyle("-fx-text-fill: " + color + ";");
        valorLabel.setWrapText(true);

        card.getChildren().addAll(lblTitulo, valorLabel);
        return card;
    }

    private GridPane construirGridGraficas() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Configurar columnas (2 columnas en primera fila, 1 columna completa en
        // segunda)
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Wrap charts en contenedores con título
        VBox containerTemporal = crearContainerGrafica("\uD83D\uDCC8 Evolución Temporal de Ingresos", chartTemporal); // 📈
        VBox containerComparativo = crearContainerGrafica("\uD83D\uDCCA Comparativa por Período", chartComparativo); // 📊
        VBox containerDistribucion = crearContainerGrafica("\uD83D\uDCCA Distribución por Tipo de Pago",
                chartDistribucion); // 📊

        grid.add(containerTemporal, 0, 0);
        grid.add(containerComparativo, 1, 0);
        grid.add(containerDistribucion, 0, 1, 2, 1); // Span 2 columnas

        return grid;
    }

    private VBox crearContainerGrafica(String titulo, Chart grafica) {
        VBox container = new VBox(12);
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 15, 0, 0, 3);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitulo.setStyle("-fx-text-fill: #343a40;");

        grafica.setPrefHeight(300);
        VBox.setVgrow(grafica, Priority.ALWAYS);

        container.getChildren().addAll(lblTitulo, grafica);
        return container;
    }

    private VBox construirPanelTabla() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 15, 0, 0, 3);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;");

        // Usar Unicode escape para evitar problemas de encoding con emoji
        Label lblTitulo = new Label("\uD83D\uDCCB Detalle de Transacciones"); // 📋
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitulo.setStyle("-fx-text-fill: #343a40;");

        configurarTabla();
        // AUMENTAR tamaño de la tabla para mostrar más transacciones
        tableTransacciones.setPrefHeight(600); // Antes: 400
        tableTransacciones.setMinHeight(500);
        VBox.setVgrow(tableTransacciones, Priority.ALWAYS);

        panel.getChildren().addAll(lblTitulo, tableTransacciones);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private void configurarTabla() {
        // Columnas
        TableColumn<TransactionSummary, OffsetDateTime> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(OffsetDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DATETIME_FORMAT));
                }
            }
        });
        colFecha.setPrefWidth(140);

        TableColumn<TransactionSummary, String> colReferencia = new TableColumn<>("Referencia");
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referenciaExterna"));
        colReferencia.setPrefWidth(120);

        TableColumn<TransactionSummary, String> colUsuario = new TableColumn<>("Usuario");
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuarioNombre"));
        colUsuario.setPrefWidth(180);

        TableColumn<TransactionSummary, String> colDocumento = new TableColumn<>("Documento");
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("usuarioDocumento"));
        colDocumento.setPrefWidth(100);

        TableColumn<TransactionSummary, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoPago"));
        colTipo.setPrefWidth(100);

        TableColumn<TransactionSummary, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colMetodo.setPrefWidth(120);

        TableColumn<TransactionSummary, BigDecimal> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colMonto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("$" + String.format("%,.0f", item.doubleValue()));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
                }
            }
        });
        colMonto.setPrefWidth(120);

        TableColumn<TransactionSummary, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "APROBADO" -> "-fx-text-fill: #28a745; -fx-font-weight: bold;";
                        case "REVERTIDO" -> "-fx-text-fill: #fd7e14; -fx-font-weight: bold;";
                        case "RECHAZADO" -> "-fx-text-fill: #dc3545; -fx-font-weight: bold;";
                        case "PENDIENTE" -> "-fx-text-fill: #ffc107; -fx-font-weight: bold;";
                        default -> "-fx-text-fill: #6c757d;";
                    };
                    setStyle(color);
                }
            }
        });
        colEstado.setPrefWidth(100);

        tableTransacciones.getColumns().addAll(
                colFecha, colReferencia, colUsuario, colDocumento,
                colTipo, colMetodo, colMonto, colEstado);

        tableTransacciones.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;");
    }

    private LineChart<String, Number> crearChartTemporal() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Fecha");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Monto ($)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(true);
        chart.setAnimated(true);
        chart.setStyle("-fx-background-color: transparent;");

        return chart;
    }

    private BarChart<String, Number> crearChartComparativo() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Período");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Monto ($)");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("");
        chart.setLegendVisible(true);
        chart.setAnimated(true);
        chart.setStyle("-fx-background-color: transparent;");

        return chart;
    }

    private PieChart crearChartDistribucion() {
        PieChart chart = new PieChart();
        chart.setTitle("");
        chart.setLegendVisible(true);
        chart.setAnimated(true);
        chart.setStyle("-fx-background-color: transparent;");
        chart.setLabelsVisible(true);

        return chart;
    }

    private void configurarEventos() {
        btnHoy.setOnAction(e -> establecerRangoHoy());
        btnSemana.setOnAction(e -> establecerRangoSemana());
        btnMes.setOnAction(e -> establecerRangoMes());
        btnAnio.setOnAction(e -> establecerRangoAnio());
        btnActualizar.setOnAction(e -> actualizarDatos());

        btnExportPDF.setOnAction(e -> {
            if (onExportarPDF != null)
                onExportarPDF.run();
        });

        btnExportCSV.setOnAction(e -> {
            if (onExportarCSV != null)
                onExportarCSV.run();
        });
    }

    private void establecerFechasPorDefecto() {
        establecerRangoMes();
    }

    private void establecerRangoHoy() {
        LocalDate hoy = LocalDate.now();
        filtroDesde.setValue(hoy);
        filtroHasta.setValue(hoy);
    }

    private void establecerRangoSemana() {
        LocalDate hoy = LocalDate.now();
        filtroDesde.setValue(hoy.minusDays(7));
        filtroHasta.setValue(hoy);
    }

    private void establecerRangoMes() {
        LocalDate hoy = LocalDate.now();
        filtroDesde.setValue(hoy.withDayOfMonth(1));
        filtroHasta.setValue(hoy);
    }

    private void establecerRangoAnio() {
        LocalDate hoy = LocalDate.now();
        filtroDesde.setValue(hoy.withDayOfYear(1));
        filtroHasta.setValue(hoy);
    }

    private void actualizarDatos() {
        LocalDate desde = filtroDesde.getValue();
        LocalDate hasta = filtroHasta.getValue();

        if (onActualizarFiltros != null) {
            onActualizarFiltros.accept(desde, hasta);
        }
    }

    /**
     * Actualiza la vista con los datos del reporte financiero.
     */
    public void actualizarVistaConDatos(FinancialReportData reporte) {
        this.reporteActual = reporte;

        // Actualizar período
        lblPeriodo.setText("Período: " + reporte.getPeriodoDescripcion());

        // Actualizar cards de métricas con animación
        actualizarCardConAnimacion(lblIngresosTotales,
                "$" + String.format("%,.0f", reporte.getIngresosTotales().doubleValue()));
        actualizarCardConAnimacion(lblDevolucionesTotales,
                "$" + String.format("%,.0f", reporte.getDevolucionesTotales().doubleValue()));
        actualizarCardConAnimacion(lblIngresosNetos,
                "$" + String.format("%,.0f", reporte.getIngresosNetos().doubleValue()));
        actualizarCardConAnimacion(lblRentabilidad,
                String.format("%.2f%%", reporte.getPorcentajeRentabilidad().doubleValue()));

        lblTransaccionesTotal.setText(String.valueOf(reporte.getTotalTransacciones()));
        lblTransaccionesAprobadas.setText(String.valueOf(reporte.getTransaccionesAprobadas()));
        lblTransaccionesRevertidas.setText(String.valueOf(reporte.getTransaccionesRevertidas()));
        lblTicketPromedio.setText("$" + String.format("%,.0f",
                reporte.getTicketPromedio() != null ? reporte.getTicketPromedio().doubleValue() : 0));

        // Actualizar gráficas
        actualizarChartTemporal(reporte.getIngresosDiarios());
        actualizarChartComparativo(reporte.getIngresosDiarios());
        actualizarChartDistribucion(reporte.getDistribucionPorTipo());

        // Actualizar tabla
        actualizarTabla(reporte.getTransacciones());
    }

    private void actualizarCardConAnimacion(Label label, String nuevoValor) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), label);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            label.setText(nuevoValor);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fade.play();
    }

    private void actualizarChartTemporal(List<DailyRevenue> datos) {
        if (datos == null || datos.isEmpty()) {
            chartTemporal.getData().clear();
            return;
        }

        XYChart.Series<String, Number> serieIngresos = new XYChart.Series<>();
        serieIngresos.setName("Ingresos");

        XYChart.Series<String, Number> serieDevoluciones = new XYChart.Series<>();
        serieDevoluciones.setName("Devoluciones");

        XYChart.Series<String, Number> serieNeto = new XYChart.Series<>();
        serieNeto.setName("Neto");

        for (DailyRevenue dr : datos) {
            String fecha = dr.getFecha().format(DATE_FORMAT);
            serieIngresos.getData().add(new XYChart.Data<>(fecha, dr.getIngresos().doubleValue()));
            serieDevoluciones.getData().add(new XYChart.Data<>(fecha, dr.getDevoluciones().doubleValue()));
            serieNeto.getData().add(new XYChart.Data<>(fecha, dr.getNeto().doubleValue()));
        }

        chartTemporal.getData().clear();
        chartTemporal.getData().add(serieIngresos);
        chartTemporal.getData().add(serieDevoluciones);
        chartTemporal.getData().add(serieNeto);
    }

    private void actualizarChartComparativo(List<DailyRevenue> datos) {
        if (datos == null || datos.isEmpty()) {
            chartComparativo.getData().clear();
            return;
        }

        // Agrupar por semana para simplificar visualización
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ingresos Netos");

        // Tomar máximo 10 puntos para no saturar
        int step = Math.max(1, datos.size() / 10);
        for (int i = 0; i < datos.size(); i += step) {
            DailyRevenue dr = datos.get(i);
            String fecha = dr.getFecha().format(DATE_FORMAT);
            serie.getData().add(new XYChart.Data<>(fecha, dr.getNeto().doubleValue()));
        }

        chartComparativo.getData().clear();
        chartComparativo.getData().add(serie);
    }

    private void actualizarChartDistribucion(List<PaymentTypeDistribution> datos) {
        chartDistribucion.getData().clear(); // Limpiar primero

        if (datos == null || datos.isEmpty()) {
            System.out.println("[DEBUG] No hay datos de distribución para mostrar en PieChart");
            // Mostrar mensaje de "Sin datos"
            chartDistribucion.setTitle("Sin datos disponibles");
            return;
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (PaymentTypeDistribution dist : datos) {
            String label = String.format("%s (%.1f%%)", dist.getTipoPago(),
                    dist.getPorcentaje() != null ? dist.getPorcentaje().doubleValue() : 0.0);
            double valor = dist.getMonto() != null ? dist.getMonto().doubleValue() : 0.0;

            if (valor > 0) { // Solo agregar si hay valor positivo
                pieChartData.add(new PieChart.Data(label, valor));
                System.out.println("[DEBUG] PieChart - Agregado: " + label + " = $" + valor);
            }
        }

        if (!pieChartData.isEmpty()) {
            chartDistribucion.setData(pieChartData);
            chartDistribucion.setTitle(""); // Limpiar título si hay datos
            System.out.println("[DEBUG] PieChart actualizado con " + pieChartData.size() + " elementos");
        } else {
            chartDistribucion.setTitle("Sin transacciones");
        }
    }

    private void actualizarTabla(List<TransactionSummary> transacciones) {
        if (transacciones == null) {
            tableTransacciones.getItems().clear();
            return;
        }

        ObservableList<TransactionSummary> items = FXCollections.observableArrayList(transacciones);
        tableTransacciones.setItems(items);
    }

    private void estilizarDatePicker(DatePicker dp) {
        dp.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;");
    }

    private void estilizarBotonRapido(Button btn) {
        btn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f8f9fa 0%, #e9ecef 100%);" +
                        "-fx-text-fill: #495057;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #007bff 0%, #0056b3 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #007bff;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;"));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f8f9fa 0%, #e9ecef 100%);" +
                        "-fx-text-fill: #495057;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;"));
    }

    private void estilizarBotonExportar(Button btn, String color, String colorHover) {
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, " + color + " 0%, " + colorHover
                        + " 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
    }

    // Setters para callbacks
    public void setOnActualizarFiltros(BiConsumer<LocalDate, LocalDate> callback) {
        this.onActualizarFiltros = callback;
    }

    public void setOnExportarPDF(Runnable callback) {
        this.onExportarPDF = callback;
    }

    public void setOnExportarCSV(Runnable callback) {
        this.onExportarCSV = callback;
    }

    public FinancialReportData getReporteActual() {
        return reporteActual;
    }
}
