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
    final TextField filtroDocumento = new TextField();
    final TextField filtroNombre = new TextField();
    final ComboBox<String> filtroTipo = new ComboBox<>();
    final CheckBox filtroActivos = new CheckBox("Solo activos");
    final Button buscarBtn = new Button("Buscar");
    final Button limpiarBtn = new Button("Limpiar");

    // Tabla y paginación
    final TableView<UserApiClient.UsuarioDto> table = new TableView<>();
    final Pagination pagination = new Pagination(1, 0);

    // Acciones
    final Button crearBtn = new Button("Nuevo");
    final Button editarBtn = new Button("Editar");
    final Button desactivarBtn = new Button("Desactivar");
    final Button biometriaBtn = new Button("Biometría...");
    final Label status = new Label();

    public UserManagementView() {
        setPadding(new Insets(10));

        // filtros
        GridPane filters = new GridPane();
        filters.setHgap(10); filters.setVgap(6);
        filtroDocumento.setPromptText("Documento");
        filtroNombre.setPromptText("Nombre contiene...");
        filtroTipo.getItems().addAll("", "NINO", "ESTUDIANTE", "DOCENTE", "PERSONAL");
        filtroTipo.setEditable(false); filtroTipo.setValue("");

        filters.add(new Label("Documento:"), 0, 0); filters.add(filtroDocumento, 1, 0);
        filters.add(new Label("Nombre:"), 2, 0); filters.add(filtroNombre, 3, 0);
        filters.add(new Label("Tipo:"), 4, 0); filters.add(filtroTipo, 5, 0);
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
        colActivo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Boolean.TRUE.equals(c.getValue().activo)?"Sí":"No"));

        table.getColumns().addAll(colDoc, colNom, colTipo, colEmail, colTel, colActivo);
    // Para compatibilidad amplia con versiones de JavaFX, usar la política clásica
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // centro: tabla + paginación
        BorderPane center = new BorderPane(table);
        center.setBottom(pagination);
        setCenter(center);

        // acciones
        HBox actions = new HBox(8, crearBtn, editarBtn, desactivarBtn, biometriaBtn);
        BorderPane bottom = new BorderPane();
        bottom.setLeft(actions);
        bottom.setRight(status);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        setBottom(bottom);
    }

    public void setTableData(List<UserApiClient.UsuarioDto> usuarios) {
        table.getItems().setAll(usuarios);
    }

    public UserApiClient.UsuarioDto getSelected() { return table.getSelectionModel().getSelectedItem(); }
}
