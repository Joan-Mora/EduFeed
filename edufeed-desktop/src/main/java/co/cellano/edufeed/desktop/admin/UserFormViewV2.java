package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Formulario modernizado para creación/edición de usuarios
 * con validación mejorada y diseño card-based.
 */
public class UserFormViewV2 extends VBox {
    public interface OnSubmit {
        void accept(UserApiClient.UsuarioDto dto);
    }

    final TextField documento = new TextField();
    final TextField nombre = new TextField();
    final ComboBox<String> tipo = new ComboBox<>();
    final TextField email = new TextField();
    final TextField telefono = new TextField();
    final CheckBox activo = new CheckBox("Usuario activo");
    final Button guardar = new Button("💾 Guardar");
    final Button cancelar = new Button("✖ Cancelar");
    final Label status = new Label();

    private final OnSubmit handler;
    private boolean readOnly = false;

    public UserFormViewV2(OnSubmit handler) {
        this.handler = handler;
        setSpacing(16);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: -fx-surface;");
        setMinWidth(500);

        buildForm();
    }

    private void buildForm() {
        // Título
        Label title = new Label("Información del Usuario");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -fx-text-primary;");

        // Grid con campos
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(16, 0, 0, 0));

        // Configurar campos
        documento.setPromptText("Ej: 1234567890");
        documento.getStyleClass().add("app-form__input");
        documento.setPrefWidth(400);

        nombre.setPromptText("Nombre completo del usuario");
        nombre.getStyleClass().add("app-form__input");
        nombre.setPrefWidth(400);

        tipo.getItems().addAll("NINO", "ESTUDIANTE", "DOCENTE", "PERSONAL");
        tipo.setPromptText("Seleccione tipo de usuario");
        tipo.getStyleClass().add("app-form__input");
        tipo.setPrefWidth(400);

        email.setPromptText("correo@ejemplo.com");
        email.getStyleClass().add("app-form__input");
        email.setPrefWidth(400);

        telefono.setPromptText("Ej: +57 300 1234567");
        telefono.getStyleClass().add("app-form__input");
        telefono.setPrefWidth(400);

        activo.setSelected(true);
        activo.setStyle("-fx-font-size: 13px;");

        // Labels con estilo
        int row = 0;
        addFormField(grid, "📄 Documento", "Número de identificación único", documento, row++);
        addFormField(grid, "👤 Nombre Completo", "Nombre y apellidos del usuario", nombre, row++);
        addFormField(grid, "🏷 Tipo de Usuario", "Rol o categoría del usuario", tipo, row++);
        addFormField(grid, "📧 Email", "Correo electrónico (opcional)", email, row++);
        addFormField(grid, "📱 Teléfono", "Número de contacto (opcional)", telefono, row++);

        VBox activoBox = new VBox(6);
        activoBox.getChildren().addAll(
                createLabel("✅ Estado", "Indica si el usuario puede acceder al sistema"),
                activo);
        grid.add(activoBox, 0, row++, 2, 1);

        // Botones de acción
        guardar.getStyleClass().addAll("app-button", "app-button--primary", "app-button--large");
        cancelar.getStyleClass().addAll("app-button", "app-button--secondary");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().addAll(cancelar, guardar);
        actions.setPadding(new Insets(16, 0, 0, 0));

        // Status bar
        status.setStyle("-fx-text-fill: -fx-error; -fx-font-size: 13px; -fx-font-weight: 500;");

        getChildren().addAll(
                title,
                new Separator(),
                grid,
                status,
                new Separator(),
                actions);

        // Eventos
        guardar.setOnAction(e -> handleSave());
    }

    private void addFormField(GridPane grid, String labelText, String help, Control field, int row) {
        VBox container = new VBox(4);
        Label label = createLabel(labelText, help);
        container.getChildren().addAll(label, field);
        grid.add(container, 0, row, 2, 1);
    }

    private Label createLabel(String text, String help) {
        Label label = new Label(text);
        label.getStyleClass().add("app-form__label");
        if (help != null && !help.isEmpty()) {
            Tooltip tooltip = new Tooltip(help);
            tooltip.getStyleClass().add("app-tooltip");
            label.setTooltip(tooltip);
        }
        return label;
    }

    private void handleSave() {
        if (readOnly) {
            status.setText("⚠ Este formulario es de solo lectura");
            return;
        }

        String msg = validate();
        if (msg != null) {
            status.setText("⚠ " + msg);
            return;
        }

        UserApiClient.UsuarioDto dto = new UserApiClient.UsuarioDto();
        dto.documento = documento.getText().trim();
        dto.nombreCompleto = nombre.getText().trim();
        dto.tipoUsuario = tipo.getValue();
        dto.email = email.getText() != null && !email.getText().trim().isEmpty()
                ? email.getText().trim()
                : null;
        dto.telefono = telefono.getText() != null && !telefono.getText().trim().isEmpty()
                ? telefono.getText().trim()
                : null;
        dto.activo = activo.isSelected();

        handler.accept(dto);
    }

    public void load(UserApiClient.UsuarioDto dto) {
        if (dto == null)
            return;
        documento.setText(dto.documento);
        nombre.setText(dto.nombreCompleto);
        tipo.setValue(dto.tipoUsuario);
        email.setText(dto.email);
        telefono.setText(dto.telefono);
        activo.setSelected(Boolean.TRUE.equals(dto.activo));
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        documento.setEditable(!readOnly);
        nombre.setEditable(!readOnly);
        tipo.setDisable(readOnly);
        email.setEditable(!readOnly);
        telefono.setEditable(!readOnly);
        activo.setDisable(readOnly);
        guardar.setVisible(!readOnly);
        guardar.setManaged(!readOnly);

        if (readOnly) {
            guardar.setText("👁 Solo Lectura");
        }
    }

    public Button getCancelarBtn() {
        return cancelar;
    }

    private String validate() {
        if (documento.getText() == null || documento.getText().trim().isEmpty())
            return "El documento es requerido";
        if (documento.getText().length() > 50)
            return "El documento es demasiado largo (máx 50 caracteres)";

        if (nombre.getText() == null || nombre.getText().trim().isEmpty())
            return "El nombre completo es requerido";
        if (nombre.getText().length() > 200)
            return "El nombre es demasiado largo (máx 200 caracteres)";

        if (tipo.getValue() == null || tipo.getValue().isBlank())
            return "Debe seleccionar un tipo de usuario";

        String emailText = email.getText();
        if (emailText != null && !emailText.trim().isEmpty()) {
            if (!emailText.contains("@") || !emailText.contains("."))
                return "El email no tiene un formato válido";
            if (emailText.length() > 200)
                return "El email es demasiado largo (máx 200 caracteres)";
        }

        if (telefono.getText() != null && telefono.getText().length() > 30)
            return "El teléfono es demasiado largo (máx 30 caracteres)";

        return null;
    }
}
