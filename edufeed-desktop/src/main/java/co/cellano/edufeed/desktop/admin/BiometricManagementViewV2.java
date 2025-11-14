package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Vista modernizada para gestión de plantillas biométricas
 * con diseño card-based y acciones WebAuthn integradas.
 */
public class BiometricManagementViewV2 extends VBox {
    // Header con ID de usuario
    public final TextField userIdField = new TextField();
    public final Button copyIdBtn = new Button("📋 Copiar ID");

    // Tabla de plantillas
    public final TableView<UserApiClient.PlantillaBiometricaDto> table = new TableView<>();

    // Controles de enrolamiento
    public final ComboBox<UserApiClient.Modalidad> modalidad = new ComboBox<>();
    public final Button enrolarBtn = new Button("➕ Enrolar");
    public final Button desactivarBtn = new Button("🗑 Desactivar");
    public final Button registrarWebAuthnBtn = new Button("📱 WebAuthn (Teléfono)");

    // Estado
    public final Label status = new Label();

    public BiometricManagementViewV2() {
        setSpacing(16);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: -fx-surface;");
        setMinWidth(800);
        setMinHeight(500);

        buildView();
    }

    private void buildView() {
        // === HEADER ===
        Label title = new Label("🔐 Gestión Biométrica");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -fx-text-primary;");

        // Card de ID de usuario
        VBox idCard = new VBox(8);
        idCard.getStyleClass().add("app-card");
        idCard.setPadding(new Insets(12));
        idCard.setStyle("-fx-background-color: derive(-fx-primary, 90%);");

        Label idLabel = new Label("UUID del Usuario:");
        idLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -fx-text-secondary;");

        userIdField.setEditable(false);
        userIdField.setPromptText("ID se cargará automáticamente");
        userIdField.getStyleClass().add("app-form__input");
        userIdField.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 11px;");

        copyIdBtn.getStyleClass().addAll("app-button", "app-button--small", "app-button--secondary");

        HBox idRow = new HBox(8, userIdField, copyIdBtn);
        idRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userIdField, Priority.ALWAYS);

        idCard.getChildren().addAll(idLabel, idRow);

        // === TABLA DE PLANTILLAS ===
        VBox tableCard = new VBox(0);
        tableCard.getStyleClass().add("app-card");
        tableCard.setStyle("-fx-padding: 0;");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        table.getStyleClass().add("app-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No hay plantillas biométricas registradas"));
        table.setMinHeight(250);
        table.setPrefHeight(Region.USE_COMPUTED_SIZE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().id));
        colId.setPrefWidth(280);
        colId.setStyle("-fx-font-size: 11px; -fx-font-family: 'Consolas', 'Monaco', monospace;");

        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colMod = new TableColumn<>("Modalidad");
        colMod.setCellValueFactory(c -> {
            String icon = getModalidadIcon(c.getValue().modalidad);
            return new javafx.beans.property.SimpleStringProperty(icon + " " + c.getValue().modalidad);
        });
        colMod.setPrefWidth(150);

        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colProv = new TableColumn<>("Proveedor");
        colProv.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().proveedor));
        colProv.setPrefWidth(120);

        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colCre = new TableColumn<>("Fecha de Registro");
        colCre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().creadoEn));
        colCre.setPrefWidth(150);

        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colAct = new TableColumn<>("Estado");
        colAct.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().activo) ? "✅ Activo" : "❌ Inactivo"));
        colAct.setPrefWidth(100);
        colAct.setStyle("-fx-alignment: CENTER;");

        table.getColumns().add(colId);
        table.getColumns().add(colMod);
        table.getColumns().add(colProv);
        table.getColumns().add(colCre);
        table.getColumns().add(colAct);

        tableCard.getChildren().add(table);

        // === BARRA DE ACCIONES ===
        VBox actionsCard = new VBox(12);
        actionsCard.getStyleClass().add("app-card");
        actionsCard.setPadding(new Insets(14));
        actionsCard.setStyle("-fx-background-color: derive(-fx-surface, 98%);");

        Label actionsTitle = new Label("⚙ Acciones de Enrolamiento");
        actionsTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -fx-text-secondary;");

        modalidad.getItems().addAll(
                UserApiClient.Modalidad.HUELLA,
                UserApiClient.Modalidad.ROSTRO,
                UserApiClient.Modalidad.VOZ);
        modalidad.setValue(UserApiClient.Modalidad.HUELLA);
        modalidad.getStyleClass().add("app-form__input");
        modalidad.setPrefWidth(200);

        // Configurar celda personalizada para el ComboBox
        modalidad.setCellFactory(cb -> new ListCell<UserApiClient.Modalidad>() {
            @Override
            protected void updateItem(UserApiClient.Modalidad item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getModalidadIcon(item.name()) + " " + item.name());
                }
            }
        });
        modalidad.setButtonCell(new ListCell<UserApiClient.Modalidad>() {
            @Override
            protected void updateItem(UserApiClient.Modalidad item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getModalidadIcon(item.name()) + " " + item.name());
                }
            }
        });

        enrolarBtn.getStyleClass().addAll("app-button", "app-button--primary");
        desactivarBtn.getStyleClass().addAll("app-button", "app-button--danger");
        registrarWebAuthnBtn.getStyleClass().addAll("app-button", "app-button--accent");

        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label modalidadLbl = new Label("Modalidad:");
        modalidadLbl.getStyleClass().add("app-form__label");
        row1.getChildren().addAll(modalidadLbl, modalidad, enrolarBtn);

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().addAll(desactivarBtn, registrarWebAuthnBtn);

        status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-secondary;");

        actionsCard.getChildren().addAll(
                actionsTitle,
                new Separator(),
                row1,
                row2,
                new Separator(),
                status);

        // Layout principal
        getChildren().addAll(
                title,
                new Separator(),
                idCard,
                tableCard,
                actionsCard);

        // Eventos
        copyIdBtn.setOnAction(e -> doCopyUserId());
    }

    private String getModalidadIcon(String modalidad) {
        if (modalidad == null)
            return "🔒";
        return switch (modalidad.toUpperCase()) {
            case "HUELLA" -> "👆";
            case "ROSTRO" -> "😊";
            case "VOZ" -> "🎤";
            case "MANUAL" -> "✍";
            default -> "🔒";
        };
    }

    public void setUserId(String userId) {
        userIdField.setText(userId != null ? userId : "");
    }

    private void doCopyUserId() {
        String txt = userIdField.getText();
        if (txt == null || txt.isBlank()) {
            status.setText("⚠ No hay ID para copiar");
            status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-warning;");
            return;
        }
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(txt);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        status.setText("✅ ID copiado al portapapeles");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
    }
}
