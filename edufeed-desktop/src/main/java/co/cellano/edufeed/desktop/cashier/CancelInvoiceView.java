package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient.PagoEnriquecidoDto;
import co.cellano.edufeed.desktop.service.PaymentApiClient.EstadoPago;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * Vista para cancelar/revertir facturas.
 * Permite buscar facturas por referencia, documento o nombre de usuario.
 */
public class CancelInvoiceView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern MOTIVO_PATTERN = Pattern.compile("\\\"motivo\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");

    // Búsqueda
    private final ComboBox<String> tipoBusquedaCombo = new ComboBox<>();
    private final TextField busquedaField = new TextField();
    private final Button buscarBtn = new Button("Buscar");

    // Resultados
    private final VBox resultadosContainer = new VBox(10);
    private final ScrollPane resultadosScrollPane = new ScrollPane();
    private final Label resultadosLabel = new Label("Seleccione una factura para ver detalles");

    // Detalles de factura seleccionada
    private final VBox detallesBox = new VBox(10);
    private final Label referenciaLabel = new Label();
    private final Label usuarioLabel = new Label();
    private final Label montoLabel = new Label();
    private final Label tipoLabel = new Label();
    private final Label estadoLabel = new Label();
    private final Label fechaLabel = new Label();
    private final Label vigenciaLabel = new Label();
    private final Label motivoPagoLabel = new Label(); // Motivo original del pago
    private final TextArea motivoCancelacionArea = new TextArea(); // Motivo de cancelación
    private final Label motivoCancelacionLabelValue = new Label(); // Muestra motivo cuando ya está rechazado
    private final Button editarMotivoBtn = new Button("✎ Editar motivo");
    private final Button guardarMotivoBtn = new Button("💾 Guardar motivo");

    // Acciones
    private final Button cancelarFacturaBtn = new Button("Cancelar Factura");
    private final Button regresarBtn = new Button("← Regresar al Menú");

    // Callbacks
    private Consumer<PagoEnriquecidoDto> onBuscarFacturas;
    private Consumer<PagoEnriquecidoDto> onCancelarFactura;
    private Runnable onRegresar;

    private PagoEnriquecidoDto facturaSeleccionada;

    public CancelInvoiceView() {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f8f9fa 0%, #e9ecef 100%);");

        construirUI();
    }

    private void construirUI() {
        // Header con título y botón regresar
        HBox headerBox = construirHeader();

        // Panel de búsqueda
        VBox busquedaBox = construirPanelBusqueda();

        // Panel de resultados
        VBox resultadosBox = construirPanelResultados();

        // Panel de detalles
        VBox detallesPanel = construirPanelDetalles();

        // Panel de acciones
        HBox accionesBox = construirPanelAcciones();

        getChildren().addAll(headerBox, busquedaBox, resultadosBox, detallesPanel, accionesBox);
    }

    private HBox construirHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));

        // Título principal
        Label titulo = new Label("🚫 Cancelar Factura");
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: 700;");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        // Botón regresar consistente con otras vistas (usar campo existente
        // regresarBtn)
        regresarBtn.setText("← Regresar");
        // Limpiar estilos previos si los hubiera y añadir clases de estilo coherentes
        regresarBtn.getStyleClass().clear();
        regresarBtn.getStyleClass().addAll("app-button", "app-button--secondary");
        regresarBtn.setOnAction(e -> {
            if (onRegresar != null)
                onRegresar.run();
        });

        header.getChildren().addAll(titulo, regresarBtn);
        return header;
    }

    private VBox construirPanelBusqueda() {
        VBox box = new VBox(15);
        box.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f8f9fa 100%);" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 25;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 3);" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;");
        box.setMaxWidth(800);

        // Título con ícono
        HBox tituloBox = new HBox(10);
        tituloBox.setAlignment(Pos.CENTER_LEFT);
        Label icono = new Label("🔍");
        icono.setFont(Font.font(24));
        Label titulo = new Label("Buscar Factura");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titulo.setStyle("-fx-text-fill: #343a40;");
        tituloBox.getChildren().addAll(icono, titulo);

        // Separador decorativo
        Region separador = new Region();
        separador.setMaxWidth(Double.MAX_VALUE);
        separador.setMinHeight(2);
        separador.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, transparent 0%, #007bff 50%, transparent 100%);");

        // Tipo de búsqueda con estilo mejorado
        Label tipoBusquedaLabel = new Label("Tipo de búsqueda:");
        tipoBusquedaLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        tipoBusquedaLabel.setStyle("-fx-text-fill: #495057;");

        tipoBusquedaCombo.getItems().addAll("Referencia de Factura", "Documento Usuario", "Nombre Usuario");
        tipoBusquedaCombo.setValue("Referencia de Factura");
        tipoBusquedaCombo.setMaxWidth(Double.MAX_VALUE);
        tipoBusquedaCombo.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-padding: 12;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-color: white;");

        // Campo de búsqueda con efecto focus
        Label busquedaLabel = new Label("Criterio:");
        busquedaLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        busquedaLabel.setStyle("-fx-text-fill: #495057;");

        busquedaField.setPromptText("Ingrese el criterio de búsqueda...");
        busquedaField.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-padding: 12;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-color: white;");
        busquedaField.setOnAction(e -> realizarBusqueda());
        busquedaField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                busquedaField.setStyle(
                        "-fx-font-size: 15px;" +
                                "-fx-padding: 12;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: #007bff;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-color: white;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.25), 8, 0, 0, 0);");
            } else {
                busquedaField.setStyle(
                        "-fx-font-size: 15px;" +
                                "-fx-padding: 12;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: #ced4da;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-color: white;");
            }
        });

        // Botón buscar con gradiente y efectos
        buscarBtn.setText("🔎 Buscar Factura");
        buscarBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #007bff 0%, #0056b3 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14 30;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.4), 10, 0, 0, 3);");
        buscarBtn.setOnAction(e -> realizarBusqueda());
        buscarBtn.setMaxWidth(Double.MAX_VALUE);
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

        HBox.setHgrow(busquedaField, Priority.ALWAYS);

        box.getChildren().addAll(tituloBox, separador, tipoBusquedaLabel, tipoBusquedaCombo, busquedaLabel,
                busquedaField, buscarBtn);
        return box;
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
        box.setMaxWidth(1200); // Aumentado de 800 a 1200 para aprovechar más espacio horizontal
        box.setMinHeight(500); // Altura mínima para garantizar espacio
        // Permitir que crezca según el espacio disponible
        VBox.setVgrow(box, Priority.ALWAYS);

        // Título con ícono
        HBox tituloBox = new HBox(10);
        tituloBox.setAlignment(Pos.CENTER_LEFT);
        Label icono = new Label("📋");
        icono.setFont(Font.font(24));
        resultadosLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        resultadosLabel.setStyle("-fx-text-fill: #343a40;");
        resultadosLabel.setText("Resultados");
        tituloBox.getChildren().addAll(icono, resultadosLabel);

        // Separador decorativo
        Region separador = new Region();
        separador.setMaxWidth(Double.MAX_VALUE);
        separador.setMinHeight(2);
        separador.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, transparent 0%, #17a2b8 50%, transparent 100%);");

        // Configurar contenedor de resultados con scroll
        resultadosContainer.setPadding(new Insets(5));
        resultadosContainer.setStyle("-fx-background-color: white;");

        resultadosScrollPane.setContent(resultadosContainer);
        resultadosScrollPane.setFitToWidth(true);
        resultadosScrollPane.setPrefHeight(500); // Aumentado de 300 a 500 para mostrar más facturas
        resultadosScrollPane.setMinHeight(400); // Altura mínima garantizada
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

    private VBox construirPanelDetalles() {
        detallesBox.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #fff5f5 100%);" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 25;" +
                        "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.2), 15, 0, 0, 3);" +
                        "-fx-border-color: #f8d7da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 16;");
        detallesBox.setMaxWidth(800);
        detallesBox.setVisible(false);
        // Para que no consuma espacio cuando está oculto
        detallesBox.setManaged(false);

        // Título con ícono
        HBox tituloBox = new HBox(10);
        tituloBox.setAlignment(Pos.CENTER_LEFT);
        Label icono = new Label("▶");
        icono.setFont(Font.font(28));
        icono.setStyle("-fx-text-fill: #dc3545;");
        Label titulo = new Label("Detalles de Factura");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titulo.setStyle("-fx-text-fill: #721c24;");
        tituloBox.getChildren().addAll(icono, titulo);

        // Separador decorativo
        Region separador = new Region();
        separador.setMaxWidth(Double.MAX_VALUE);
        separador.setMinHeight(2);
        separador.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, transparent 0%, #dc3545 50%, transparent 100%);");

        // Configurar labels de detalle con estilo mejorado
        configurarLabelDetalle(referenciaLabel);
        configurarLabelDetalle(usuarioLabel);
        configurarLabelDetalle(montoLabel);
        configurarLabelDetalle(tipoLabel);
        configurarLabelDetalle(estadoLabel);
        configurarLabelDetalle(fechaLabel);
        configurarLabelDetalle(vigenciaLabel);
        configurarLabelDetalle(motivoPagoLabel);

        // Área de motivo de cancelación
        motivoCancelacionArea.setPromptText("Escriba el motivo de la cancelación (opcional)...");
        motivoCancelacionArea.setPrefRowCount(3);
        motivoCancelacionArea.setWrapText(true);
        motivoCancelacionArea.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 12;" +
                        "-fx-background-color: white;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;");
        motivoCancelacionArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                motivoCancelacionArea.setStyle(
                        "-fx-font-size: 14px;" +
                                "-fx-padding: 12;" +
                                "-fx-background-color: white;" +
                                "-fx-border-color: #dc3545;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-radius: 10;" +
                                "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.25), 8, 0, 0, 0);");
            } else {
                motivoCancelacionArea.setStyle(
                        "-fx-font-size: 14px;" +
                                "-fx-padding: 12;" +
                                "-fx-background-color: white;" +
                                "-fx-border-color: #ced4da;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-radius: 10;");
            }
        });

        Label motivoCancelacionLabel = new Label("✎ Motivo de cancelación:");
        motivoCancelacionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        motivoCancelacionLabel.setStyle("-fx-text-fill: #495057;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);
        grid.setStyle("-fx-padding: 10 0;");

        // Configurar labels con iconos
        String[][] campos = {
                { "📄", "Referencia:", "0" },
                { "👤", "Usuario:", "1" },
                { "💰", "Monto:", "2" },
                { "📦", "Tipo:", "3" },
                { "🔖", "Estado:", "4" },
                { "📅", "Fecha:", "5" },
                { "⏰", "Vigencia:", "6" },
                { "📝", "Motivo de Pago:", "7" }
        };

        Label[] valueLabels = { referenciaLabel, usuarioLabel, montoLabel, tipoLabel, estadoLabel, fechaLabel,
                vigenciaLabel, motivoPagoLabel };

        for (int i = 0; i < campos.length; i++) {
            Label keyLabel = new Label(campos[i][0] + " " + campos[i][1]);
            keyLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            keyLabel.setStyle("-fx-text-fill: #495057;");
            keyLabel.setMinWidth(150);
            grid.add(keyLabel, 0, i);
            grid.add(valueLabels[i], 1, i);
        }

        // Botón editar motivo (inicialmente oculto)
        editarMotivoBtn.setVisible(false);
        editarMotivoBtn.setManaged(false);
        editarMotivoBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #ffc107 0%, #e0a800 100%);" +
                        "-fx-text-fill: #212529;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(255,193,7,0.4), 8, 0, 0, 2);");

        HBox motivoCancelacionMostrar = new HBox(10);
        motivoCancelacionMostrar.setAlignment(Pos.CENTER_LEFT);
        motivoCancelacionLabelValue.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        motivoCancelacionLabelValue.setStyle("-fx-text-fill: #212529;");
        motivoCancelacionMostrar.getChildren().addAll(motivoCancelacionLabelValue, editarMotivoBtn);

        // Botón guardar motivo (solo visible al editar)
        guardarMotivoBtn.setVisible(false);
        guardarMotivoBtn.setManaged(false);
        guardarMotivoBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #28a745 0%, #1e7e34 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 18;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(40,167,69,0.4), 8, 0, 0, 2);");

        detallesBox.getChildren().addAll(tituloBox, separador, grid, motivoCancelacionLabel, motivoCancelacionArea,
                guardarMotivoBtn, motivoCancelacionMostrar);
        return detallesBox;
    }

    private void configurarLabelDetalle(Label label) {
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        label.setStyle("-fx-text-fill: #212529;");
    }

    private HBox construirPanelAcciones() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15, 0, 0, 0));

        // Botón cancelar factura con gradiente rojo
        cancelarFacturaBtn.setText("🚫 Cancelar Factura");
        cancelarFacturaBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #dc3545 0%, #bd2130 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14 35;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.4), 12, 0, 0, 3);");
        cancelarFacturaBtn.setDisable(true);
        cancelarFacturaBtn.setOnAction(e -> confirmarCancelacion());
        cancelarFacturaBtn.setOnMouseEntered(e -> {
            if (!cancelarFacturaBtn.isDisabled()) {
                cancelarFacturaBtn.setStyle(
                        "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #c82333 0%, #a71d2a 100%);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 14 35;" +
                                "-fx-background-radius: 10;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.6), 15, 0, 0, 4);" +
                                "-fx-scale-x: 1.03;" +
                                "-fx-scale-y: 1.03;");
            }
        });
        cancelarFacturaBtn.setOnMouseExited(e -> {
            if (!cancelarFacturaBtn.isDisabled()) {
                cancelarFacturaBtn.setStyle(
                        "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #dc3545 0%, #bd2130 100%);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 14 35;" +
                                "-fx-background-radius: 10;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.4), 12, 0, 0, 3);");
            }
        });

        box.getChildren().add(cancelarFacturaBtn);
        return box;
    }

    private void realizarBusqueda() {
        String criterio = busquedaField.getText();
        if (criterio == null || criterio.isBlank()) {
            mostrarAlerta("Error", "Ingrese un criterio de búsqueda", Alert.AlertType.WARNING);
            return;
        }

        // Limpiar selección anterior
        limpiarDetalles();

        // Callback para realizar búsqueda (implementado en CashierModule)
        if (onBuscarFacturas != null) {
            onBuscarFacturas.accept(null); // El module usará el criterio de búsqueda desde los campos públicos
        }
    }

    private void mostrarDetalles(PagoEnriquecidoDto pago) {
        referenciaLabel.setText(pago.referenciaExterna != null ? pago.referenciaExterna : "N/A");
        usuarioLabel.setText(String.format("%s (%s)",
                pago.usuarioNombre != null ? pago.usuarioNombre : "N/A",
                pago.usuarioDocumento != null ? pago.usuarioDocumento : "N/A"));
        montoLabel.setText(pago.monto != null ? "$" + String.format("%,.0f", pago.monto) : "$0");
        tipoLabel.setText(pago.tipoPago != null ? pago.tipoPago.name() : "N/A");

        String estadoTexto = pago.estadoPago != null ? pago.estadoPago.name() : "DESCONOCIDO";
        String colorEstado = switch (estadoTexto) {
            case "APROBADO" -> "-fx-text-fill: #28a745; -fx-font-weight: bold;";
            case "PENDIENTE" -> "-fx-text-fill: #ffc107; -fx-font-weight: bold;";
            case "RECHAZADO" -> "-fx-text-fill: #dc3545; -fx-font-weight: bold;";
            default -> "-fx-text-fill: #6c757d; -fx-font-weight: bold;";
        };
        estadoLabel.setText(estadoTexto);
        estadoLabel.setStyle(colorEstado);

        fechaLabel.setText(pago.creadoEn != null ? pago.creadoEn.format(DATE_FORMAT) : "N/A");

        String vigencia = "N/A";
        if (pago.vigenteDesde != null && pago.vigenteHasta != null) {
            vigencia = String.format("%s a %s",
                    pago.vigenteDesde.format(DATE_FORMAT),
                    pago.vigenteHasta.format(DATE_FORMAT));
        }
        vigenciaLabel.setText(vigencia);

        // Mostrar motivo del pago original (metadatos)
        if (pago.metadatos != null && !pago.metadatos.isBlank()) {
            String motivoPago = extraerMotivoDeMetadatos(pago.metadatos);
            motivoPagoLabel.setText(motivoPago);
        } else {
            motivoPagoLabel.setText("Sin motivo registrado");
        }

        // Mostrar/ocultar controles según estado
        if (pago.estadoPago == EstadoPago.RECHAZADO) {
            // El motivo de cancelación se guarda en metadatos con clave motivo_cancelacion
            // si existe
            String motivoCancelacion = extraerMotivoCancelacion(pago.metadatos);
            motivoCancelacionLabelValue
                    .setText(motivoCancelacion != null ? motivoCancelacion : "(Sin motivo registrado)");
            motivoCancelacionArea.setVisible(false);
            motivoCancelacionArea.setManaged(false);
            editarMotivoBtn.setVisible(true);
            editarMotivoBtn.setManaged(true);
            editarMotivoBtn.setOnAction(e -> habilitarEdicionMotivo(pago));
        } else {
            motivoCancelacionArea.clear();
            motivoCancelacionArea.setEditable(true);
            motivoCancelacionArea.setVisible(true);
            motivoCancelacionArea.setManaged(true);
            editarMotivoBtn.setVisible(false);
            editarMotivoBtn.setManaged(false);
            guardarMotivoBtn.setVisible(false);
            guardarMotivoBtn.setManaged(false);
            motivoCancelacionLabelValue.setText("");
        }

        detallesBox.setManaged(true);
        detallesBox.setVisible(true);

        // Permitir cancelar pagos PENDIENTES y APROBADOS (no RECHAZADOS)
        boolean puedeCancelar = pago.estadoPago == EstadoPago.APROBADO || pago.estadoPago == EstadoPago.PENDIENTE;
        cancelarFacturaBtn.setDisable(!puedeCancelar);

        if (!puedeCancelar && pago.estadoPago != null) {
            cancelarFacturaBtn.setText("✗ No se puede cancelar (" + pago.estadoPago.name() + ")");
        } else {
            cancelarFacturaBtn.setText("✗ Cancelar Factura");
        }
    }

    private void confirmarCancelacion() {
        if (facturaSeleccionada == null)
            return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Cancelación");
        confirmacion.setHeaderText("¿Está seguro de cancelar esta factura?");
        confirmacion.setContentText(String.format(
                "Referencia: %s\nUsuario: %s\nMonto: $%,.0f\n\nEsta acción no se puede deshacer.",
                facturaSeleccionada.referenciaExterna,
                facturaSeleccionada.usuarioNombre,
                facturaSeleccionada.monto));

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (onCancelarFactura != null) {
                    onCancelarFactura.accept(facturaSeleccionada);
                }
            }
        });
    }

    private void limpiarDetalles() {
        facturaSeleccionada = null;
        detallesBox.setVisible(false);
        detallesBox.setManaged(false);
        referenciaLabel.setText("");
        usuarioLabel.setText("");
        montoLabel.setText("");
        tipoLabel.setText("");
        estadoLabel.setText("");
        fechaLabel.setText("");
        vigenciaLabel.setText("");
        motivoPagoLabel.setText("");
        motivoCancelacionArea.clear();
        motivoCancelacionArea.setEditable(true);
        motivoCancelacionArea.setVisible(true);
        motivoCancelacionArea.setManaged(true);
        editarMotivoBtn.setVisible(false);
        editarMotivoBtn.setManaged(false);
        motivoCancelacionLabelValue.setText("");
        guardarMotivoBtn.setVisible(false);
        guardarMotivoBtn.setManaged(false);
        cancelarFacturaBtn.setDisable(true);
        cancelarFacturaBtn.setText("✗ Cancelar Factura");
    }

    public void setResultados(List<PagoEnriquecidoDto> facturas) {
        resultadosContainer.getChildren().clear();
        if (facturas == null || facturas.isEmpty()) {
            resultadosLabel.setText("No se encontraron facturas");
            limpiarDetalles();

            // Mostrar mensaje de vacío
            Label emptyLabel = new Label("No hay facturas para mostrar");
            emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-padding: 20;");
            resultadosContainer.getChildren().add(emptyLabel);
        } else {
            resultadosLabel.setText(String.format("Se encontraron %d factura(s)", facturas.size()));

            // Crear una card para cada factura
            for (PagoEnriquecidoDto pago : facturas) {
                VBox card = crearCardFactura(pago);
                resultadosContainer.getChildren().add(card);
            }
        }
    }

    private VBox crearCardFactura(PagoEnriquecidoDto pago) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16)); // Aumentado de 12 a 16 para más espaciado
        card.setMinHeight(90); // Altura mínima para cada card
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;");

        // Información principal en HBox
        HBox infoBox = new HBox(15);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        String estado = pago.estadoPago != null ? pago.estadoPago.name() : "DESCONOCIDO";
        String colorBadge = switch (estado) {
            case "APROBADO" -> "#28a745";
            case "PENDIENTE" -> "#ffc107";
            case "RECHAZADO" -> "#dc3545";
            default -> "#6c757d";
        };
        String iconoEstado = switch (estado) {
            case "APROBADO" -> "✓";
            case "PENDIENTE" -> "⏳";
            case "RECHAZADO" -> "✗";
            default -> "?";
        };

        String fecha = pago.creadoEn != null ? pago.creadoEn.format(DATE_FORMAT) : "N/A";
        String usuario = pago.usuarioNombre != null ? pago.usuarioNombre : "N/A";
        String monto = pago.monto != null ? "$" + String.format("%,.0f", pago.monto) : "$0";

        // Referencia
        VBox refBox = new VBox(4);
        Label refTitulo = new Label("Referencia");
        refTitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-weight: 600;");
        Label refValor = new Label("📄 " + (pago.referenciaExterna != null ? pago.referenciaExterna : "N/A"));
        refValor.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); // Aumentado de 13 a 14
        refValor.setStyle("-fx-text-fill: #212529;");
        refValor.setWrapText(true); // Permitir wrap si es muy largo
        refBox.getChildren().addAll(refTitulo, refValor);
        refBox.setMinWidth(250); // Ancho mínimo para referencia
        HBox.setHgrow(refBox, Priority.ALWAYS);

        // Usuario
        VBox userBox = new VBox(4);
        Label userTitulo = new Label("Usuario");
        userTitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-weight: 600;");
        Label userValor = new Label("👤 " + usuario);
        userValor.setFont(Font.font("Segoe UI", 14)); // Aumentado de 13 a 14
        userValor.setStyle("-fx-text-fill: #495057;");
        userValor.setWrapText(true);
        userBox.getChildren().addAll(userTitulo, userValor);
        userBox.setMinWidth(220); // Ancho mínimo para usuario
        HBox.setHgrow(userBox, Priority.ALWAYS);

        // Monto
        VBox montoBox = new VBox(4);
        Label montoTitulo = new Label("Monto");
        montoTitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-font-weight: 600;");
        Label montoValor = new Label("💰 " + monto);
        montoValor.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); // Aumentado de 14 a 16
        montoValor.setStyle("-fx-text-fill: #007bff;");
        montoBox.getChildren().addAll(montoTitulo, montoValor);
        montoBox.setMinWidth(140); // Ancho mínimo para monto

        // Estado badge
        Label estadoBadge = new Label(iconoEstado + " " + estado);
        estadoBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); // Aumentado de 11 a 12
        estadoBadge.setStyle(
                "-fx-background-color: " + colorBadge + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 8 18;" + // Aumentado padding de 6-14 a 8-18
                        "-fx-background-radius: 14;"); // Aumentado de 12 a 14
        estadoBadge.setMinWidth(120); // Ancho mínimo para el badge

        infoBox.getChildren().addAll(refBox, userBox, montoBox, estadoBadge);

        // Información adicional en segunda línea
        HBox infoAdicional = new HBox(20);
        infoAdicional.setAlignment(Pos.CENTER_LEFT);

        Label fechaLabel = new Label("📅 Creado: " + fecha);
        fechaLabel.setFont(Font.font("Segoe UI", 12)); // Aumentado de 11 a 12
        fechaLabel.setStyle("-fx-text-fill: #6c757d;");

        // Mostrar tipo de pago si está disponible
        String tipoPagoTexto = pago.tipoPago != null ? pago.tipoPago.name() : "";
        if (!tipoPagoTexto.isEmpty()) {
            Label tipoLabel = new Label("📦 Tipo: " + tipoPagoTexto);
            tipoLabel.setFont(Font.font("Segoe UI", 12));
            tipoLabel.setStyle("-fx-text-fill: #6c757d;");
            infoAdicional.getChildren().addAll(fechaLabel, tipoLabel);
        } else {
            infoAdicional.getChildren().add(fechaLabel);
        }

        card.getChildren().addAll(infoBox, infoAdicional);

        // Efectos hover y click
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-border-color: #007bff;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.3), 10, 0, 0, 2);"));

        card.setOnMouseExited(e -> {
            if (facturaSeleccionada != pago) {
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: #dee2e6;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-radius: 10;" +
                                "-fx-cursor: hand;");
            }
        });

        card.setOnMouseClicked(e -> {
            facturaSeleccionada = pago;
            mostrarDetalles(pago);

            // Resaltar card seleccionada
            resultadosContainer.getChildren().forEach(node -> {
                if (node instanceof VBox) {
                    node.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-border-color: #dee2e6;" +
                                    "-fx-border-width: 2;" +
                                    "-fx-border-radius: 10;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-cursor: hand;");
                }
            });

            card.setStyle(
                    "-fx-background-color: #e7f3ff;" +
                            "-fx-border-color: #007bff;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,123,255,0.4), 12, 0, 0, 3);");
        });

        return card;
    }

    public void limpiarFormulario() {
        busquedaField.clear();
        tipoBusquedaCombo.setValue("Referencia de Factura");
        resultadosContainer.getChildren().clear();
        resultadosLabel.setText("Seleccione una factura para ver detalles");
        limpiarDetalles();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Extrae el motivo en texto limpio desde los metadatos.
     * Estrategia:
     * 1. Intentar parsear como JSON y buscar clave "motivo" (recursivo).
     * 2. Usar regex como respaldo.
     * 3. Si no se encuentra, devolver la cadena sin llaves/artículos JSON
     * superficiales.
     */
    private String extraerMotivoDeMetadatos(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return "Sin motivo registrado";
        }
        String raw = metadatos.trim();

        // 1. Intentar parsear con Jackson
        if (raw.startsWith("{") || raw.startsWith("{")) {
            try {
                var tree = JSON.readTree(raw);
                String encontrado = buscarMotivoRecursivo(tree);
                if (encontrado != null && !encontrado.isBlank()) {
                    return limpiarTexto(encontrado);
                }
            } catch (Exception ignore) {
                // Continuar con siguientes estrategias
            }
        }

        // 2. Regex respaldo
        var matcher = MOTIVO_PATTERN.matcher(raw);
        if (matcher.find()) {
            return limpiarTexto(matcher.group(1));
        }

        // 3. Si parece JSON pero sin clave motivo, intentar quitar llaves y comillas
        // externas
        if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
            String simplificado = raw.replaceAll("^[\\{\\[]", "").replaceAll("[\\}\\]]$", "").trim();
            // Quitar pares clave-valor si solo hay uno
            if (simplificado.contains(":")) {
                // E.g. "motivo=Compra" -> separar tras = o :
                int idx = simplificado.indexOf(":");
                if (idx != -1 && idx + 1 < simplificado.length()) {
                    String posible = simplificado.substring(idx + 1).replaceAll("^[\"']|[\"']$", "").trim();
                    if (!posible.isBlank()) {
                        return limpiarTexto(posible);
                    }
                }
            }
        }

        // 4. Retornar texto plano limpiado de secuencias escapadas
        return limpiarTexto(raw);
    }

    private String limpiarTexto(String texto) {
        return texto
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    private String buscarMotivoRecursivo(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null)
            return null;
        if (node.has("motivo")) {
            var val = node.get("motivo");
            if (val != null && val.isTextual()) {
                return val.asText();
            }
        }
        // Recorrer hijos objeto
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                var nested = buscarMotivoRecursivo(entry.getValue());
                if (nested != null)
                    return nested;
            }
        } else if (node.isArray()) {
            for (var child : node) {
                var nested = buscarMotivoRecursivo(child);
                if (nested != null)
                    return nested;
            }
        }
        return null;
    }

    // Getters públicos para que CashierModule acceda a los criterios de búsqueda
    public String getTipoBusqueda() {
        return tipoBusquedaCombo.getValue();
    }

    public String getCriterioBusqueda() {
        return busquedaField.getText();
    }

    public String getMotivoCancelacion() {
        return motivoCancelacionArea.getText();
    }

    // Extrae motivo_cancelacion del JSON de metadatos si existe
    private String extraerMotivoCancelacion(String metadatos) {
        if (metadatos == null || metadatos.isBlank())
            return null;
        try {
            var node = JSON.readTree(metadatos);
            if (node.has("motivo_cancelacion") && node.get("motivo_cancelacion").isTextual()) {
                return limpiarTexto(node.get("motivo_cancelacion").asText());
            }
        } catch (Exception ignore) {
        }
        // Fallback simple
        if (metadatos.contains("motivo_cancelacion")) {
            int idx = metadatos.indexOf("motivo_cancelacion");
            int colon = metadatos.indexOf(':', idx);
            if (colon != -1) {
                int quoteStart = metadatos.indexOf('"', colon);
                int quoteEnd = quoteStart != -1 ? metadatos.indexOf('"', quoteStart + 1) : -1;
                if (quoteStart != -1 && quoteEnd != -1) {
                    return limpiarTexto(metadatos.substring(quoteStart + 1, quoteEnd));
                }
            }
        }
        return null;
    }

    // Habilita edición del motivo ya existente
    private void habilitarEdicionMotivo(PagoEnriquecidoDto pago) {
        motivoCancelacionArea.setVisible(true);
        motivoCancelacionArea.setManaged(true);
        editarMotivoBtn.setVisible(false);
        editarMotivoBtn.setManaged(false);
        guardarMotivoBtn.setVisible(true);
        guardarMotivoBtn.setManaged(true);
        String existente = extraerMotivoCancelacion(pago.metadatos);
        if (existente != null) {
            motivoCancelacionArea.setText(existente);
        }
        guardarMotivoBtn.setOnAction(e -> {
            if (onGuardarMotivoCancelacion != null && facturaSeleccionada != null) {
                onGuardarMotivoCancelacion.accept(facturaSeleccionada, motivoCancelacionArea.getText());
            }
        });
    }

    // Callback para guardar el motivo de cancelación editado
    private java.util.function.BiConsumer<PagoEnriquecidoDto, String> onGuardarMotivoCancelacion;

    public void setOnGuardarMotivoCancelacion(java.util.function.BiConsumer<PagoEnriquecidoDto, String> cb) {
        this.onGuardarMotivoCancelacion = cb;
    }

    // Setters para callbacks
    public void setOnBuscarFacturas(Consumer<PagoEnriquecidoDto> callback) {
        this.onBuscarFacturas = callback;
    }

    public void setOnCancelarFactura(Consumer<PagoEnriquecidoDto> callback) {
        this.onCancelarFactura = callback;
    }

    public void setOnRegresar(Runnable callback) {
        this.onRegresar = callback;
    }
}
