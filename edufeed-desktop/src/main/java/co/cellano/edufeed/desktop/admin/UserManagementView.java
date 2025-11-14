package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class UserManagementView extends BorderPane {
    // Filtros
    public final TextField filtroDocumento = new TextField();
    public final TextField filtroNombre = new TextField();
    public final ComboBox<String> filtroTipo = new ComboBox<>();
    public final CheckBox filtroActivos = new CheckBox("Solo activos");
    public final Button buscarBtn = new Button("Buscar");
    public final Button limpiarBtn = new Button("Limpiar");

    // Tabla y paginación
    public final TableView<UserApiClient.UsuarioDto> table = new TableView<>();
    public final Pagination pagination = new Pagination(1, 0);

    // Acciones
    public final Button crearBtn = new Button("Nuevo");
    public final Button verBtn = new Button("Ver");
    public final Button editarBtn = new Button("Editar");
    public final Button toggleActivoBtn = new Button("Activar/Desactivar");
    public final Button eliminarBtn = new Button("Eliminar");
    public final Button biometriaBtn = new Button("Biometría...");
    public final SplitMenuButton registrarBioBtn = new SplitMenuButton();
    public final MenuItem registrarCompletoItem = new MenuItem("Completo (Huella, FaceID, Voz)");
    public final MenuItem registrarHuellaItem = new MenuItem("Registrar Huella");
    public final MenuItem registrarFaceItem = new MenuItem("Registrar FaceID");
    public final MenuItem registrarVozItem = new MenuItem("Registrar Voz");
    public final Label status = new Label();
    public final HBox bioChips = new HBox(6);

    public UserManagementView() {
        setPadding(new Insets(10));

        // filtros
        GridPane filters = new GridPane();
        filters.getStyleClass().add("app-card");
        filters.setHgap(10);
        filters.setVgap(6);
        filtroDocumento.setPromptText("Documento");
        filtroNombre.setPromptText("Nombre contiene...");
        filtroTipo.getItems().addAll("NINO", "ESTUDIANTE", "DOCENTE", "PERSONAL");
        filtroTipo.setEditable(false);
        filtroTipo.setPromptText("Todos");

        // Hacer el checkbox visible con estilos explícitos
        filtroActivos.setStyle("-fx-text-fill: -fx-text; -fx-font-size: 13px;");

        filters.add(new Label("Documento:"), 0, 0);
        filters.add(filtroDocumento, 1, 0);
        filters.add(new Label("Nombre:"), 2, 0);
        filters.add(filtroNombre, 3, 0);
        filters.add(new Label("Tipo:"), 4, 0);
        filters.add(filtroTipo, 5, 0);
        filters.add(filtroActivos, 6, 0);
        HBox searchActions = new HBox(8, buscarBtn, limpiarBtn);
        filters.add(searchActions, 7, 0);
        setTop(filters);

        // tabla
        TableColumn<UserApiClient.UsuarioDto, String> colDoc = new TableColumn<>("Documento");
        colDoc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().documento));
        TableColumn<UserApiClient.UsuarioDto, String> colNom = new TableColumn<>("Nombre");
        colNom.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().nombreCompleto));
        TableColumn<UserApiClient.UsuarioDto, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().tipoUsuario));
        TableColumn<UserApiClient.UsuarioDto, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().email));
        TableColumn<UserApiClient.UsuarioDto, String> colTel = new TableColumn<>("Teléfono");
        colTel.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().telefono));
        TableColumn<UserApiClient.UsuarioDto, String> colActivo = new TableColumn<>("Activo");
        colActivo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().activo) ? "Sí" : "No"));

        // Añadir columnas de forma segura (suprime warning de varargs genéricos)
        table.getColumns().add(colDoc);
        table.getColumns().add(colNom);
        table.getColumns().add(colTipo);
        table.getColumns().add(colEmail);
        table.getColumns().add(colTel);
        table.getColumns().add(colActivo);
        table.getStyleClass().add("app-table");
        // Para compatibilidad amplia con versiones de JavaFX, usar la política clásica
        // Política moderna: usar UNCONSTRAINED_RESIZE y permitir ajuste manual si se
        // desea
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // centro: tabla + paginación
        BorderPane center = new BorderPane(table);
        center.setBottom(pagination);
        setCenter(center);

        // acciones
        registrarBioBtn.setText("Registrar Datos Biométricos");
        // Estilos modernos a botones
        crearBtn.getStyleClass().addAll("app-button", "app-button--primary");
        verBtn.getStyleClass().addAll("app-button", "app-button--ghost");
        editarBtn.getStyleClass().addAll("app-button", "app-button--secondary");
        toggleActivoBtn.getStyleClass().addAll("app-button", "app-button--accent");
        eliminarBtn.getStyleClass().addAll("app-button", "app-button--danger");
        biometriaBtn.getStyleClass().addAll("app-button", "app-button--secondary");
        registrarBioBtn.getStyleClass().addAll("app-button", "app-button--primary");
        registrarBioBtn.getItems().addAll(registrarCompletoItem, new SeparatorMenuItem(),
                registrarHuellaItem, registrarFaceItem, registrarVozItem);
        HBox actions = new HBox(8,
                crearBtn,
                verBtn,
                editarBtn,
                toggleActivoBtn,
                eliminarBtn,
                biometriaBtn,
                registrarBioBtn);
        BorderPane bottom = new BorderPane();
        bottom.setLeft(actions);
        // Centro: chips biométricos
        HBox centerBox = new HBox(bioChips);
        centerBox.setSpacing(8);
        bottom.setCenter(centerBox);
        // Derecha: estado textual
        bottom.setRight(status);
        bottom.getStyleClass().add("app-statusbar");
        bottom.setPadding(new Insets(8, 0, 0, 0));
        setBottom(bottom);
    }

    public void setTableData(List<UserApiClient.UsuarioDto> usuarios) {
        table.getItems().setAll(usuarios);
    }

    public UserApiClient.UsuarioDto getSelected() {
        return table.getSelectionModel().getSelectedItem();
    }
}
