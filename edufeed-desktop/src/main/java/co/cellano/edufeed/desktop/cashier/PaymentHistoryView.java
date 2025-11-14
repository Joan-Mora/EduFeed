package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient.EstadoPago;
import co.cellano.edufeed.desktop.service.PaymentApiClient.PagoEnriquecidoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Vista de historial de pagos con filtros avanzados y visualización tipo cards.
 */
public class PaymentHistoryView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern MOTIVO_PATTERN = Pattern.compile("\\\"motivo\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");

    // Filtros
    private final TextField filtroNombre = new TextField();
    private final TextField filtroDocumento = new TextField();
    private final TextField filtroReferencia = new TextField();
    private final DatePicker filtroDesde = new DatePicker();
    private final DatePicker filtroHasta = new DatePicker();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final Button buscarBtn = new Button("🔍 Buscar");
    private final Button limpiarBtn = new Button("🗑 Limpiar Filtros");
    private final Button regresarBtn = new Button("← Regresar");

    // Resultados
    private final VBox resultadosContainer = new VBox(10);
    private final ScrollPane resultadosScrollPane = new ScrollPane();
    private final Label resultadosLabel = new Label("Historial de Pagos");

    // Callbacks
    private Runnable onBuscar;
    private Consumer<PagoEnriquecidoDto> onRevertir;
    private Consumer<PagoEnriquecidoDto> onEditarMotivoDevolucion;
    private Runnable onRegresar;

    private List<PagoEnriquecidoDto> todosLosPagos = new ArrayList<>();

    public PaymentHistoryView() {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f8f9fa 0%, #e9ecef 100%);");

        construirUI();
    }

    private void construirUI() {
        // Header con título y botón regresar
        HBox headerBox = construirHeader();

        // Panel de filtros
        VBox filtrosBox = construirPanelFiltros();

        // Panel de resultados
        VBox resultadosBox = construirPanelResultados();

        getChildren().addAll(headerBox, filtrosBox, resultadosBox);
    }

    private HBox construirHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label titulo = new Label("📊 Historial de Pagos");
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: 700;");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        regresarBtn.setText("← Regresar");
        regresarBtn.getStyleClass().clear();
        regresarBtn.getStyleClass().addAll("app-button", "app-button--secondary");
        regresarBtn.setOnAction(e -> {
            if (onRegresar != null)
                onRegresar.run();
        });

        header.getChildren().addAll(titulo, regresarBtn);
        return header;
    }

    private VBox construirPanelFiltros() {
        VBox box = new VBox(15);
        box.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f8f9fa 100%);" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 25;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;");
        box.setMaxWidth(1200);

        // Título con ícono
        HBox tituloBox = new HBox(10);
        tituloBox.setAlignment(Pos.CENTER_LEFT);
        Label icono = new Label("🔍");
        icono.setFont(Font.font(24));
        Label titulo = new Label("Filtros de Búsqueda");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titulo.setStyle("-fx-text-fill: #343a40;");
        tituloBox.getChildren().addAll(icono, titulo);

        // Separador decorativo
        Region separador = new Region();
        separador.setMaxWidth(Double.MAX_VALUE);
        separador.setMinHeight(2);
        separador.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, transparent 0%, #17a2b8 50%, transparent 100%);");

        // Primera fila: Nombre, Documento, Referencia
        HBox fila1 = new HBox(12);
        fila1.setAlignment(Pos.CENTER_LEFT);

        VBox nombreBox = crearCampoFiltro("👤 Nombre:", filtroNombre, "Buscar por nombre...", 250);
        VBox documentoBox = crearCampoFiltro("🆔 Documento:", filtroDocumento, "Buscar por documento...", 180);
        VBox referenciaBox = crearCampoFiltro("📄 Referencia:", filtroReferencia, "Buscar por referencia...", 200);

        fila1.getChildren().addAll(nombreBox, documentoBox, referenciaBox);

        // Segunda fila: Fechas y Estado
        HBox fila2 = new HBox(12);
        fila2.setAlignment(Pos.CENTER_LEFT);

        VBox desdeBox = crearCampoFechaFiltro("📅 Desde:", filtroDesde);
        VBox hastaBox = crearCampoFechaFiltro("📅 Hasta:", filtroHasta);

        Label estadoLabel = new Label("🔖 Estado:");
        estadoLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        estadoLabel.setStyle("-fx-text-fill: #495057;");
        filtroEstado.getItems().addAll("Todos", "PENDIENTE", "APROBADO", "RECHAZADO", "DEVOLUCIONES");
        filtroEstado.setValue("Todos");
        filtroEstado.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-padding: 12;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-color: white;");
        filtroEstado.setPrefWidth(180);
        VBox estadoBox = new VBox(4, estadoLabel, filtroEstado);

        fila2.getChildren().addAll(desdeBox, hastaBox, estadoBox);

        // Botones
        HBox botonesBox = new HBox(12);
        botonesBox.setAlignment(Pos.CENTER_LEFT);
        botonesBox.setPadding(new Insets(10, 0, 0, 0));

        buscarBtn.setText("🔎 Buscar");
        buscarBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #007bff 0%, #0056b3 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14 30;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.4), 10, 0, 0, 3);");
        buscarBtn.setOnAction(e -> aplicarFiltros());
        buscarBtn.setOnMouseEntered(e -> buscarBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #0056b3 0%, #004085 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14 30;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.6), 12, 0, 0, 4);" +
                        "-fx-scale-x: 1.02;" +
                        "-fx-scale-y: 1.02;"));
        buscarBtn.setOnMouseExited(e -> buscarBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #007bff 0%, #0056b3 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14 30;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.4), 10, 0, 0, 3);"));

        limpiarBtn.setText("🗑 Limpiar Filtros");
        limpiarBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #6c757d 0%, #5a6268 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14 25;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(108,117,125,0.4), 8, 0, 0, 2);");
        limpiarBtn.setOnAction(e -> limpiarFiltros());

        botonesBox.getChildren().addAll(buscarBtn, limpiarBtn);

        box.getChildren().addAll(tituloBox, separador, fila1, fila2, botonesBox);
        return box;
    }

    private VBox crearCampoFiltro(String labelText, TextField campo, String prompt, double width) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        label.setStyle("-fx-text-fill: #495057;");

        campo.setPromptText(prompt);
        campo.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-padding: 12;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-color: white;");
        campo.setPrefWidth(width);
        campo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                campo.setStyle(
                        "-fx-font-size: 15px;" +
                                "-fx-padding: 12;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: #007bff;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-color: white;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.25), 8, 0, 0, 0);");
            } else {
                campo.setStyle(
                        "-fx-font-size: 15px;" +
                                "-fx-padding: 12;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: #ced4da;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-color: white;");
            }
        });

        return new VBox(4, label, campo);
    }

    private VBox crearCampoFechaFiltro(String labelText, DatePicker campo) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        label.setStyle("-fx-text-fill: #495057;");

        campo.setPromptText("Seleccionar fecha");
        campo.setPrefWidth(180);
        campo.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 8;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-color: white;");

        return new VBox(4, label, campo);
    }

    private VBox construirPanelResultados() {
        VBox box = new VBox(12);
        box.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f8f9fa 100%);" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 25;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;");
        box.setMaxWidth(1200);
        box.setMinHeight(500);
        VBox.setVgrow(box, Priority.ALWAYS);

        // Título con ícono
        HBox tituloBox = new HBox(10);
        tituloBox.setAlignment(Pos.CENTER_LEFT);
        Label icono = new Label("📋");
        icono.setFont(Font.font(24));
        resultadosLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        resultadosLabel.setStyle("-fx-text-fill: #343a40;");
        resultadosLabel.setText("Historial de Pagos");
        tituloBox.getChildren().addAll(icono, resultadosLabel);

        // Separador decorativo
        Region separador = new Region();
        separador.setMaxWidth(Double.MAX_VALUE);
        separador.setMinHeight(2);
        separador.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, transparent 0%, #28a745 50%, transparent 100%);");

        // Configurar contenedor de resultados con scroll
        resultadosContainer.setPadding(new Insets(5));
        resultadosContainer.setStyle("-fx-background-color: white;");

        resultadosScrollPane.setContent(resultadosContainer);
        resultadosScrollPane.setFitToWidth(true);
        resultadosScrollPane.setPrefHeight(500);
        resultadosScrollPane.setMinHeight(400);
        resultadosScrollPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;");

        VBox.setVgrow(resultadosScrollPane, Priority.ALWAYS);

        box.getChildren().addAll(tituloBox, separador, resultadosScrollPane);
        return box;
    }

    private void aplicarFiltros() {
        if (onBuscar != null) {
            onBuscar.run();
        }
    }

    private void limpiarFiltros() {
        filtroNombre.clear();
        filtroDocumento.clear();
        filtroReferencia.clear();
        filtroDesde.setValue(null);
        filtroHasta.setValue(null);
        filtroEstado.setValue("Todos");

        // Mostrar todos los pagos sin filtros
        mostrarResultados(todosLosPagos);
    }

    public void setResultados(List<PagoEnriquecidoDto> pagos) {
        this.todosLosPagos = new ArrayList<>(pagos);
        aplicarFiltrosLocales();
    }

    private void aplicarFiltrosLocales() {
        List<PagoEnriquecidoDto> filtrados = new ArrayList<>(todosLosPagos);

        // Filtro por nombre
        String nombre = filtroNombre.getText();
        if (nombre != null && !nombre.isBlank()) {
            String nombreLower = nombre.toLowerCase();
            filtrados.removeIf(p -> p.usuarioNombre == null || !p.usuarioNombre.toLowerCase().contains(nombreLower));
        }

        // Filtro por documento
        String documento = filtroDocumento.getText();
        if (documento != null && !documento.isBlank()) {
            filtrados.removeIf(p -> p.usuarioDocumento == null || !p.usuarioDocumento.contains(documento));
        }

        // Filtro por referencia
        String referencia = filtroReferencia.getText();
        if (referencia != null && !referencia.isBlank()) {
            String referenciaLower = referencia.toLowerCase();
            filtrados.removeIf(
                    p -> p.referenciaExterna == null || !p.referenciaExterna.toLowerCase().contains(referenciaLower));
        }

        // Filtro por estado
        String estado = filtroEstado.getValue();
        if (estado != null && !estado.equals("Todos")) {
            if (estado.equalsIgnoreCase("Devoluciones") || estado.equalsIgnoreCase("DEVOLUCIONES")) {
                // Filtrar solo pagos con estado REVERTIDO
                filtrados.removeIf(p -> p.estadoPago != EstadoPago.REVERTIDO);
            } else {
                EstadoPago estadoEnum = EstadoPago.valueOf(estado);
                filtrados.removeIf(p -> p.estadoPago != estadoEnum);
            }
        }

        // Filtro por fechas
        LocalDate desde = filtroDesde.getValue();
        LocalDate hasta = filtroHasta.getValue();
        if (desde != null || hasta != null) {
            filtrados.removeIf(p -> {
                if (p.creadoEn == null)
                    return true;
                LocalDate fecha = p.creadoEn.toLocalDate();
                if (desde != null && fecha.isBefore(desde))
                    return true;
                if (hasta != null && fecha.isAfter(hasta))
                    return true;
                return false;
            });
        }

        mostrarResultados(filtrados);
    }

    private void mostrarResultados(List<PagoEnriquecidoDto> pagos) {
        resultadosContainer.getChildren().clear();

        if (pagos.isEmpty()) {
            Label noResults = new Label("No se encontraron pagos con los filtros seleccionados");
            noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: #6c757d; -fx-padding: 40 0;");
            resultadosContainer.getChildren().add(noResults);
            resultadosLabel.setText("Historial de Pagos (0)");
            return;
        }

        resultadosLabel.setText(String.format("Historial de Pagos (%d)", pagos.size()));

        for (PagoEnriquecidoDto pago : pagos) {
            VBox card = crearCardPago(pago);
            resultadosContainer.getChildren().add(card);
        }
    }

    private VBox crearCardPago(PagoEnriquecidoDto pago) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #007bff;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.2), 12, 0, 0, 3);" +
                        "-fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"));

        // Encabezado del card con referencia, monto y estado
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label referenciaLabel = new Label("📄 " + (pago.referenciaExterna != null ? pago.referenciaExterna : "N/A"));
        referenciaLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        referenciaLabel.setStyle("-fx-text-fill: #212529;");
        HBox.setHgrow(referenciaLabel, Priority.ALWAYS);

        Label montoLabel = new Label(pago.monto != null ? "$" + String.format("%,.0f", pago.monto) : "$0");
        montoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        montoLabel.setStyle("-fx-text-fill: #28a745;");

        Label estadoLabel = new Label(pago.estadoPago != null ? pago.estadoPago.name() : "DESCONOCIDO");
        estadoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        estadoLabel.setPadding(new Insets(6, 12, 6, 12));
        estadoLabel.setStyle(getEstiloEstado(pago.estadoPago));

        header.getChildren().addAll(referenciaLabel, montoLabel, estadoLabel);

        // Información del usuario y fecha
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label usuarioLabel = new Label("👤 " + (pago.usuarioNombre != null ? pago.usuarioNombre : "N/A") +
                " (" + (pago.usuarioDocumento != null ? pago.usuarioDocumento : "N/A") + ")");
        usuarioLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        usuarioLabel.setStyle("-fx-text-fill: #495057;");

        Label fechaLabel = new Label("📅 " + (pago.creadoEn != null ? pago.creadoEn.format(DATE_FORMAT) : "N/A"));
        fechaLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        fechaLabel.setStyle("-fx-text-fill: #6c757d;");

        Label tipoLabel = new Label("📦 " + (pago.tipoPago != null ? pago.tipoPago.name() : "N/A"));
        tipoLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        tipoLabel.setStyle("-fx-text-fill: #6c757d;");

        infoBox.getChildren().addAll(usuarioLabel, fechaLabel, tipoLabel);

        // Motivos de pago y cancelación
        VBox motivosBox = new VBox(8);
        motivosBox.setPadding(new Insets(10, 0, 0, 0));

        if (pago.metadatos != null && !pago.metadatos.isBlank()) {
            String motivoPago = extraerMotivoDeMetadatos(pago.metadatos);
            if (!motivoPago.equals("Sin motivo registrado")) {
                Label motivoPagoLabel = new Label("📝 Motivo de Pago: " + motivoPago);
                motivoPagoLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
                motivoPagoLabel.setStyle("-fx-text-fill: #212529;");
                motivoPagoLabel.setWrapText(true);
                motivosBox.getChildren().add(motivoPagoLabel);
            }

            // Si está rechazado, mostrar motivo de cancelación
            if (pago.estadoPago == EstadoPago.RECHAZADO) {
                String motivoCancelacion = extraerMotivoCancelacion(pago.metadatos);
                if (motivoCancelacion != null && !motivoCancelacion.equals("(Sin motivo registrado)")) {
                    Label motivoCancelacionLabel = new Label("✎ Motivo de Cancelación: " + motivoCancelacion);
                    motivoCancelacionLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                    motivoCancelacionLabel.setStyle("-fx-text-fill: #dc3545;");
                    motivoCancelacionLabel.setWrapText(true);
                    motivosBox.getChildren().add(motivoCancelacionLabel);
                }
            }

            // Si está revertido, mostrar motivo de devolución
            if (pago.estadoPago == EstadoPago.REVERTIDO) {
                String motivoDevolucion = extraerMotivoDevolucion(pago.metadatos);
                Label motivoDevolucionLabel = new Label("🔄 Motivo de Devolución: " + motivoDevolucion);
                motivoDevolucionLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
                motivoDevolucionLabel.setStyle("-fx-text-fill: #fd7e14;");
                motivoDevolucionLabel.setWrapText(true);
                motivosBox.getChildren().add(motivoDevolucionLabel);
            }
        }

        // Botones de acción
        HBox accionesBox = new HBox(10);
        accionesBox.setAlignment(Pos.CENTER_RIGHT);
        accionesBox.setPadding(new Insets(10, 0, 0, 0));

        Button copiarBtn = new Button("📋 Copiar Referencia");
        copiarBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #17a2b8 0%, #138496 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;");
        copiarBtn.setOnAction(e -> copiarReferencia(pago.referenciaExterna));

        accionesBox.getChildren().add(copiarBtn);

        // Botón editar motivo solo para pagos REVERTIDO
        if (pago.estadoPago == EstadoPago.REVERTIDO) {
            Button editarMotivoBtn = new Button("✏️ Editar Motivo");
            editarMotivoBtn.setStyle(
                    "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #6c757d 0%, #5a6268 100%);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 8 16;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;");
            editarMotivoBtn.setOnAction(e -> {
                if (onEditarMotivoDevolucion != null) {
                    onEditarMotivoDevolucion.accept(pago);
                }
            });
            accionesBox.getChildren().add(editarMotivoBtn);
        }

        // Botón revertir solo para pagos APROBADO
        if (pago.estadoPago == EstadoPago.APROBADO) {
            Button revertirBtn = new Button("🔄 Revertir Pago");
            revertirBtn.setStyle(
                    "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffc107 0%, #e0a800 100%);" +
                            "-fx-text-fill: #212529;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 8 16;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;");
            revertirBtn.setOnAction(e -> {
                if (onRevertir != null) {
                    onRevertir.accept(pago);
                }
            });
            accionesBox.getChildren().add(revertirBtn);
        }

        card.getChildren().addAll(header, infoBox, motivosBox, accionesBox);
        return card;
    }

    private String getEstiloEstado(EstadoPago estado) {
        if (estado == null) {
            return "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 6;";
        }
        return switch (estado) {
            case APROBADO -> "-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 6;";
            case PENDIENTE -> "-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-background-radius: 6;";
            case RECHAZADO -> "-fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 6;";
            case REVERTIDO -> "-fx-background-color: #fd7e14; -fx-text-fill: white; -fx-background-radius: 6;"; // Naranja
                                                                                                                // para
                                                                                                                // devoluciones
        };
    }

    private void copiarReferencia(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Advertencia");
            alert.setHeaderText(null);
            alert.setContentText("No hay referencia para copiar");
            alert.showAndWait();
            return;
        }

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(referencia);
        clipboard.setContent(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Copiado");
        alert.setHeaderText(null);
        alert.setContentText("Referencia copiada al portapapeles: " + referencia);
        alert.showAndWait();
    }

    private String extraerMotivoDeMetadatos(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return "Sin motivo registrado";
        }

        try {
            JsonNode root = JSON.readTree(metadatos);
            String motivo = buscarMotivoRecursivo(root);
            if (motivo != null) {
                return limpiarTexto(motivo);
            }
        } catch (Exception ignored) {
        }

        Matcher matcher = MOTIVO_PATTERN.matcher(metadatos);
        if (matcher.find()) {
            return limpiarTexto(matcher.group(1));
        }

        return "Sin motivo registrado";
    }

    private String extraerMotivoCancelacion(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return null;
        }

        try {
            JsonNode root = JSON.readTree(metadatos);
            if (root.has("motivo_cancelacion")) {
                String valor = root.get("motivo_cancelacion").asText();
                return limpiarTexto(valor);
            }
        } catch (Exception ignored) {
        }

        Pattern pattern = Pattern.compile("\\\"motivo_cancelacion\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(metadatos);
        if (matcher.find()) {
            return limpiarTexto(matcher.group(1));
        }

        return null;
    }

    private String extraerMotivoDevolucion(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return "(Sin motivo registrado)";
        }

        try {
            JsonNode root = JSON.readTree(metadatos);
            if (root.has("motivo_devolucion")) {
                String valor = root.get("motivo_devolucion").asText();
                return limpiarTexto(valor);
            }
        } catch (Exception ignored) {
        }

        Pattern pattern = Pattern.compile("\\\"motivo_devolucion\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(metadatos);
        if (matcher.find()) {
            return limpiarTexto(matcher.group(1));
        }

        return "(Sin motivo registrado)";
    }

    private String buscarMotivoRecursivo(JsonNode node) {
        if (node == null)
            return null;
        if (node.has("motivo")) {
            return node.get("motivo").asText();
        }
        if (node.isObject()) {
            var it = node.fields();
            while (it.hasNext()) {
                var entry = it.next();
                String result = buscarMotivoRecursivo(entry.getValue());
                if (result != null)
                    return result;
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String result = buscarMotivoRecursivo(item);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private String limpiarTexto(String texto) {
        if (texto == null)
            return "";
        return texto.replace("\\n", " ")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    // Setters para callbacks
    public void setOnBuscar(Runnable callback) {
        this.onBuscar = callback;
    }

    public void setOnRevertir(Consumer<PagoEnriquecidoDto> callback) {
        this.onRevertir = callback;
    }

    public void setOnEditarMotivoDevolucion(Consumer<PagoEnriquecidoDto> callback) {
        this.onEditarMotivoDevolucion = callback;
    }

    public void setOnRegresar(Runnable callback) {
        this.onRegresar = callback;
    }
}
