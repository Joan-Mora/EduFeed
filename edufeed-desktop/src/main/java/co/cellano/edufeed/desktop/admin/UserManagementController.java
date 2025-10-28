package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import co.cellano.edufeed.desktop.service.UserApiClient.PageResponse;
import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserManagementController {
    private final Stage stage;
    private final UserManagementView view = new UserManagementView();
    private final UserApiClient api;

    private int currentPage = 0;
    private int pageSize = 20;

    public UserManagementController(Stage stage, String baseUrl, String bearer) {
        this.stage = stage;
        this.api = new UserApiClient(baseUrl, bearer);
        wire();
    }

    public void start() {
        stage.setScene(new Scene(view, 1100, 650));
        stage.setTitle("EduFeed — Administración de usuarios");
        stage.show();
        loadPage(0);
    }

    private void wire() {
        view.buscarBtn.setOnAction(e -> { currentPage = 0; loadPage(0); });
        view.limpiarBtn.setOnAction(e -> { view.filtroDocumento.clear(); view.filtroNombre.clear(); view.filtroTipo.setValue(""); view.filtroActivos.setSelected(false); currentPage=0; loadPage(0);} );
        view.pagination.currentPageIndexProperty().addListener((obs,ov,nv) -> loadPage(nv.intValue()));

        view.crearBtn.setOnAction(e -> openUserForm(null));
        view.editarBtn.setOnAction(e -> {
            var sel = view.getSelected(); if (sel==null) { view.status.setText("Seleccione un usuario"); return; }
            openUserForm(sel);
        });
        view.desactivarBtn.setOnAction(e -> runAsync(() -> doDesactivar()));
        view.biometriaBtn.setOnAction(e -> openBiometricDialog());
    }

    private void loadPage(int page) {
        view.status.setText("Cargando...");
        runAsync(() -> {
            try {
                var filters = new UserApiClient.Filters(
                        textVal(view.filtroDocumento), textVal(view.filtroNombre), selVal(view.filtroTipo), view.filtroActivos.isSelected());
                PageResponse<UserApiClient.UsuarioDto> p = api.listPaged(page, pageSize, filters);
                Platform.runLater(() -> {
                    view.setTableData(p.content);
                    view.pagination.setPageCount(Math.max(1, p.totalPages));
                    view.pagination.setCurrentPageIndex(Math.max(0, Math.min(p.number, p.totalPages-1)));
                    view.status.setText("Total: "+ p.totalElements);
                });
            } catch (IOException ex) {
                Platform.runLater(() -> view.status.setText("Error: "+ex.getMessage()));
            }
        });
    }

    private void openUserForm(UserApiClient.UsuarioDto existing) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.initOwner(stage); dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle(existing==null?"Crear usuario":"Editar usuario");
        UserFormView form = new UserFormView(dto -> runAsync(() -> doSubmit(existing, dto, dlg)));
        form.load(existing);
        DialogPane pane = dlg.getDialogPane();
        pane.setContent(form);
        pane.getButtonTypes().addAll(javafx.scene.control.ButtonType.CLOSE);
        dlg.show();
    }

    private void doSubmit(UserApiClient.UsuarioDto existing, UserApiClient.UsuarioDto dto, Dialog<?> dlg) {
        try {
            if (existing==null) api.create(dto); else api.update(existing.id, dto);
            Platform.runLater(() -> { dlg.close(); loadPage(0); });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error guardando: "+e.getMessage()));
        }
    }

    private void doDesactivar() {
        var sel = view.getSelected(); if (sel==null) { Platform.runLater(() -> view.status.setText("Seleccione un usuario")); return; }
        try {
            api.desactivar(sel.id);
            Platform.runLater(() -> { view.status.setText("Usuario desactivado"); loadPage(view.pagination.getCurrentPageIndex()); });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error desactivando: "+e.getMessage()));
        }
    }

    private void openBiometricDialog() {
        var sel = view.getSelected(); if (sel==null) { view.status.setText("Seleccione un usuario"); return; }
        Dialog<Void> dlg = new Dialog<>();
        dlg.initOwner(stage); dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Biometría de "+ sel.nombreCompleto);
        BiometricManagementView bio = new BiometricManagementView();
        DialogPane pane = dlg.getDialogPane(); pane.setContent(bio);
        pane.getButtonTypes().addAll(javafx.scene.control.ButtonType.CLOSE);

        bio.enrolarBtn.setOnAction(e -> runAsync(() -> doEnrolar(sel.id, bio)));
        bio.desactivarBtn.setOnAction(e -> runAsync(() -> doDesactivarPlantilla(sel.id, bio)));

        dlg.show();
        // cargar al abrir
        runAsync(() -> doCargarPlantillas(sel.id, bio));
    }

    private void doCargarPlantillas(String userId, BiometricManagementView bio) {
        try {
            List<UserApiClient.PlantillaBiometricaDto> l = api.listarBiometrias(userId);
            Platform.runLater(() -> bio.table.getItems().setAll(l));
        } catch (IOException e) {
            Platform.runLater(() -> bio.status.setText("Error cargando: "+e.getMessage()));
        }
    }

    private void doEnrolar(String userId, BiometricManagementView bio) {
        try {
            var mod = bio.modalidad.getValue();
            api.enrolar(userId, mod);
            doCargarPlantillas(userId, bio);
            Platform.runLater(() -> bio.status.setText("Enrolado " + mod));
        } catch (IOException e) {
            Platform.runLater(() -> bio.status.setText("Error enrolando: "+e.getMessage()));
        }
    }

    private void doDesactivarPlantilla(String userId, BiometricManagementView bio) {
        var sel = bio.table.getSelectionModel().getSelectedItem();
        if (sel==null) { Platform.runLater(() -> bio.status.setText("Seleccione una plantilla")); return; }
        try {
            api.desactivarBiometria(userId, sel.id);
            doCargarPlantillas(userId, bio);
            Platform.runLater(() -> bio.status.setText("Plantilla desactivada"));
        } catch (IOException e) {
            Platform.runLater(() -> bio.status.setText("Error desactivando: "+e.getMessage()));
        }
    }

    private static String textVal(javafx.scene.control.TextField tf) { return tf.getText()!=null?tf.getText().trim():""; }
    private static String selVal(javafx.scene.control.ComboBox<String> cb) { return cb.getValue()!=null?cb.getValue().trim():""; }

    private void runAsync(Runnable r) { new Thread(r, "admin").start(); }
}
