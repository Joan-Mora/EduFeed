package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient.TipoPago;
import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vista rediseñada para asignar pagos desde cero con búsqueda de usuario,
 * generación automática de referencia, y aprobación automática opcional.
 */
public class AssignPaymentView extends VBox {

    // Búsqueda de usuario
    public final TextField busquedaField;
    public final ComboBox<String> tipoBusquedaCombo;
    public final Button buscarBtn;
    public final ListView<UsuarioDto> resultadosListView;
    public final Label seleccionLabel;

    // Formulario de pago
    public final TextField montoField;
    public final ComboBox<TipoPago> tipoPagoCombo;
    public final ComboBox<String> metodoPagoCombo;
    public final DatePicker fechaLimiteField;
    public final TextField diasPaqueteField;
    public final TextArea motivoArea;
    public final TextField referenciaField;
    public final CheckBox aprobarAutoCheck;

    // Acciones
    public final Button asignarBtn;
    public final Button regresarBtn;
    public final Label statusLabel;

    private UsuarioDto usuarioSeleccionado;

    public AssignPaymentView() {
        getStyleClass().add("assign-payment-view");
        setSpacing(20);
        setPadding(new Insets(20));
        VBox.setVgrow(this, Priority.ALWAYS);

        // Encabezado
        HBox encabezado = new HBox(16);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("💰 Asignar Nuevo Pago");
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: 700;");
        HBox.setHgrow(titulo, Priority.ALWAYS);

        regresarBtn = new Button("← Regresar");
        regresarBtn.getStyleClass().addAll("app-button", "app-button--secondary");

        encabezado.getChildren().addAll(titulo, regresarBtn);

        // Sección 1: Búsqueda de Usuario
        VBox busquedaCard = new VBox(16);
        busquedaCard.getStyleClass().add("app-card");
        busquedaCard.setPadding(new Insets(20));

        Label busquedaTitulo = new Label("🔍 Buscar Usuario");
        busquedaTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: 600;");

        HBox busquedaControls = new HBox(12);
        busquedaControls.setAlignment(Pos.CENTER_LEFT);

        tipoBusquedaCombo = new ComboBox<>(FXCollections.observableArrayList("Nombre", "Cédula", "ID"));
        tipoBusquedaCombo.setValue("Nombre");
        tipoBusquedaCombo.getStyleClass().add("app-input");
        tipoBusquedaCombo.setPrefWidth(120);

        busquedaField = new TextField();
        busquedaField.setPromptText("Ingrese el criterio de búsqueda...");
        busquedaField.getStyleClass().add("app-input");
        HBox.setHgrow(busquedaField, Priority.ALWAYS);

        buscarBtn = new Button("Buscar");
        buscarBtn.getStyleClass().addAll("app-button", "app-button--primary");

        busquedaControls.getChildren().addAll(tipoBusquedaCombo, busquedaField, buscarBtn);

        // Lista de resultados
        resultadosListView = new ListView<>();
        resultadosListView.getStyleClass().add("app-list");
        resultadosListView.setPrefHeight(150);
        resultadosListView.setPlaceholder(new Label("No hay resultados"));
        resultadosListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UsuarioDto user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%s - %s (ID: %s)", user.nombreCompleto, user.documento, user.id));
                }
            }
        });

        seleccionLabel = new Label("No hay usuario seleccionado");
        seleccionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-text-secondary;");

        busquedaCard.getChildren().addAll(busquedaTitulo, busquedaControls, resultadosListView, seleccionLabel);

        // Sección 2: Datos del Pago
        VBox pagoCard = new VBox(16);
        pagoCard.getStyleClass().add("app-card");
        pagoCard.setPadding(new Insets(20));

        Label pagoTitulo = new Label("💵 Datos del Pago");
        pagoTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: 600;");

        // Grid para campos
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);

        // Monto
        Label montoLabel = new Label("Monto ($):");
        montoLabel.getStyleClass().add("app-label");
        montoField = new TextField();
        montoField.setPromptText("0.00");
        montoField.getStyleClass().add("app-input");
        grid.add(montoLabel, 0, 0);
        grid.add(montoField, 1, 0);

        // Tipo de Pago
        Label tipoLabel = new Label("Tipo de Pago:");
        tipoLabel.getStyleClass().add("app-label");
        tipoPagoCombo = new ComboBox<>(FXCollections.observableArrayList(TipoPago.values()));
        tipoPagoCombo.setValue(TipoPago.MENSUAL);
        tipoPagoCombo.getStyleClass().add("app-input");
        grid.add(tipoLabel, 0, 1);
        grid.add(tipoPagoCombo, 1, 1);

        // Método de Pago
        Label metodoLabel = new Label("Método de Pago:");
        metodoLabel.getStyleClass().add("app-label");
        metodoPagoCombo = new ComboBox<>(
                FXCollections.observableArrayList("EFECTIVO", "TARJETA", "TRANSFERENCIA", "POS"));
        metodoPagoCombo.setValue("EFECTIVO");
        metodoPagoCombo.getStyleClass().add("app-input");
        grid.add(metodoLabel, 0, 2);
        grid.add(metodoPagoCombo, 1, 2);

        // Fecha Límite de Pago
        Label fechaLabel = new Label("Fecha Límite:");
        fechaLabel.getStyleClass().add("app-label");
        fechaLimiteField = new DatePicker();
        fechaLimiteField.setValue(LocalDate.now().plusDays(7)); // Default: 7 días
        fechaLimiteField.getStyleClass().add("app-input");
        fechaLimiteField.setPromptText("Fecha límite para pagar");
        grid.add(fechaLabel, 0, 3);
        grid.add(fechaLimiteField, 1, 3);

        // Días Paquete (solo si tipo es PAQUETE)
        Label diasLabel = new Label("Días Paquete:");
        diasLabel.getStyleClass().add("app-label");
        diasPaqueteField = new TextField();
        diasPaqueteField.setPromptText("30");
        diasPaqueteField.getStyleClass().add("app-input");
        diasPaqueteField.setDisable(true);
        grid.add(diasLabel, 0, 4);
        grid.add(diasPaqueteField, 1, 4);

        // Referencia (generada automáticamente)
        Label referenciaLabel = new Label("Referencia:");
        referenciaLabel.getStyleClass().add("app-label");
        referenciaField = new TextField();
        referenciaField.setEditable(false);
        referenciaField.getStyleClass().add("app-input");
        referenciaField.setStyle("-fx-background-color: -fx-control-inner-background; -fx-opacity: 0.7;");
        generarReferencia();
        grid.add(referenciaLabel, 0, 5);
        grid.add(referenciaField, 1, 5);

        // Motivo
        Label motivoLabel = new Label("Motivo del Pago:");
        motivoLabel.getStyleClass().add("app-label");
        motivoArea = new TextArea();
        motivoArea.setPromptText("Especifique de qué trata este pago (opcional)...");
        motivoArea.getStyleClass().add("app-input");
        motivoArea.setPrefRowCount(3);
        motivoArea.setWrapText(true);
        grid.add(motivoLabel, 0, 6);
        grid.add(motivoArea, 1, 6);

        // Aprobar automáticamente
        aprobarAutoCheck = new CheckBox("Aprobar automáticamente (pago presencial)");
        aprobarAutoCheck.getStyleClass().add("app-checkbox");
        aprobarAutoCheck.setStyle("-fx-font-size: 14px;");
        grid.add(aprobarAutoCheck, 0, 7, 2, 1);

        // Configurar columnas del grid
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        pagoCard.getChildren().addAll(pagoTitulo, grid);

        // Sección 3: Acciones
        HBox accionesBox = new HBox(12);
        accionesBox.setAlignment(Pos.CENTER_RIGHT);

        asignarBtn = new Button("✓ Asignar Pago");
        asignarBtn.getStyleClass().addAll("app-button", "app-button--primary");
        asignarBtn.setStyle("-fx-font-size: 16px; -fx-padding: 12 24;");
        asignarBtn.setDisable(true); // Habilitar solo cuando hay usuario seleccionado

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 14px;");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        accionesBox.getChildren().addAll(statusLabel, asignarBtn);

        getChildren().addAll(encabezado, busquedaCard, pagoCard, accionesBox);

        // Listeners
        setupListeners();
    }

    private void setupListeners() {
        // Habilitar días paquete solo si tipo es PAQUETE
        tipoPagoCombo.valueProperty().addListener((obs, old, neo) -> {
            diasPaqueteField.setDisable(neo != TipoPago.PAQUETE);
            if (neo != TipoPago.PAQUETE) {
                diasPaqueteField.clear();
            }
        });

        // Selección de usuario
        resultadosListView.getSelectionModel().selectedItemProperty().addListener((obs, old, neo) -> {
            if (neo != null) {
                usuarioSeleccionado = neo;
                seleccionLabel.setText(String.format("✓ Seleccionado: %s - %s", neo.nombreCompleto, neo.documento));
                seleccionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: green;");
                asignarBtn.setDisable(false);
            } else {
                usuarioSeleccionado = null;
                seleccionLabel.setText("No hay usuario seleccionado");
                seleccionLabel
                        .setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-text-secondary;");
                asignarBtn.setDisable(true);
            }
        });

        // Enter en búsqueda
        busquedaField.setOnAction(e -> buscarBtn.fire());
    }

    /**
     * Genera una referencia única y aleatoria que nunca se repetirá.
     */
    private void generarReferencia() {
        // Formato: PAY-{timestamp}-{UUID corto}
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuidCorto = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        referenciaField.setText(String.format("PAY-%s-%s", timestamp, uuidCorto));
    }

    public void setResultados(List<UsuarioDto> usuarios) {
        resultadosListView.setItems(FXCollections.observableArrayList(usuarios));
        if (usuarios.isEmpty()) {
            statusLabel.setText("⚠ No se encontraron usuarios");
            statusLabel.setStyle("-fx-text-fill: orange;");
        } else {
            statusLabel.setText(String.format("✓ %d resultado(s) encontrado(s)", usuarios.size()));
            statusLabel.setStyle("-fx-text-fill: green;");
        }
    }

    public UsuarioDto getUsuarioSeleccionado() {
        return usuarioSeleccionado;
    }

    public void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }

    public void limpiarFormulario() {
        montoField.clear();
        motivoArea.clear();
        aprobarAutoCheck.setSelected(false);
        generarReferencia(); // Nueva referencia para el próximo pago
        tipoPagoCombo.setValue(TipoPago.MENSUAL);
        metodoPagoCombo.setValue("EFECTIVO");
        fechaLimiteField.setValue(LocalDate.now().plusDays(7));
        diasPaqueteField.clear();
    }

    /**
     * Datos del formulario listos para enviar.
     */
    public static record PaymentData(
            String usuarioId,
            BigDecimal monto,
            TipoPago tipo,
            String metodo,
            String referencia,
            LocalDate fechaLimite,
            Integer diasPaquete,
            String motivo,
            boolean aprobarAuto) {
    }

    public PaymentData getPaymentData() throws IllegalArgumentException {
        if (usuarioSeleccionado == null) {
            throw new IllegalArgumentException("Debe seleccionar un usuario");
        }

        String montoStr = montoField.getText().trim();
        if (montoStr.isEmpty()) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoStr);
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor a cero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El monto debe ser un número válido");
        }

        TipoPago tipo = tipoPagoCombo.getValue();
        String metodo = metodoPagoCombo.getValue();
        String referencia = referenciaField.getText();
        LocalDate fechaLimite = fechaLimiteField.getValue();
        String motivo = motivoArea.getText().trim();
        boolean aprobarAuto = aprobarAutoCheck.isSelected();

        Integer diasPaquete = null;
        if (tipo == TipoPago.PAQUETE) {
            String diasStr = diasPaqueteField.getText().trim();
            if (diasStr.isEmpty()) {
                throw new IllegalArgumentException("Debe especificar los días del paquete");
            }
            try {
                diasPaquete = Integer.parseInt(diasStr);
                if (diasPaquete <= 0) {
                    throw new IllegalArgumentException("Los días del paquete deben ser mayores a cero");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Los días del paquete deben ser un número válido");
            }
        }

        return new PaymentData(
                usuarioSeleccionado.id,
                monto,
                tipo,
                metodo,
                referencia,
                fechaLimite,
                diasPaquete,
                motivo.isEmpty() ? null : motivo,
                aprobarAuto);
    }
}
