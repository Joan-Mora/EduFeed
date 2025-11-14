package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import co.cellano.edufeed.desktop.service.UserApiClient.PageResponse;
import co.cellano.edufeed.desktop.theme.ThemeService;
import co.cellano.edufeed.desktop.util.AnimationUtils;
import java.io.IOException;
import java.util.List;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserManagementController {
    private final Stage stage;
    private final UserManagementViewV2 view = new UserManagementViewV2();
    private final UserApiClient api;
    private final Runnable onChangeModule;

    private int pageSize = 20;

    public UserManagementController(Stage stage, String baseUrl, String bearer) {
        this(stage, baseUrl, bearer, null);
    }

    public UserManagementController(Stage stage, String baseUrl, String bearer, Runnable onChangeModule) {
        this.stage = stage;
        this.api = new UserApiClient(baseUrl, bearer);
        this.onChangeModule = onChangeModule;
        wire();
    }

    public void start() {
        var root = new javafx.scene.layout.BorderPane(view);
        root.setTop(new co.cellano.edufeed.desktop.ui.NavBar("EduFeed — Administración de usuarios", onChangeModule));
        Scene scene = new Scene(root, 1200, 750);
        // Registrar escena en ThemeService (reemplaza UIUtils.applyTheme deprecado)
        ThemeService.getInstance().register(scene);
        stage.setScene(scene);
        stage.setTitle("EduFeed — Administración de usuarios");

        // Centrar ventana
        co.cellano.edufeed.desktop.util.StageUtils.centerWindow(stage, stage.getWidth(), stage.getHeight());

        stage.show();
        AnimationUtils.fadeIn(root); // reemplaza UIUtils.fadeIn deprecado

        // Cargar datos iniciales
        Platform.runLater(() -> loadPage(0));
    }

    private void wire() {
        view.buscarBtn.setOnAction(e -> loadPage(0));
        view.limpiarBtn.setOnAction(e -> {
            view.filtroDocumento.clear();
            view.filtroNombre.clear();
            // Volver al valor por defecto visible en el combo
            view.filtroTipo.getSelectionModel().select("Todos");
            view.filtroActivos.setSelected(false);
            loadPage(0);
        });
        view.pagination.currentPageIndexProperty().addListener((obs, ov, nv) -> loadPage(nv.intValue()));

        // Actualizar chips biométricos cuando cambia la selección
        view.table.getSelectionModel().selectedItemProperty()
                .addListener((obs, ov, nv) -> runAsync(this::refreshBioChips));

        view.crearBtn.setOnAction(e -> openUserForm(null));
        view.verBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("Seleccione un usuario");
                return;
            }
            openUserForm(sel); // modo lectura simple reutilizando formulario
        });
        view.editarBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("Seleccione un usuario");
                return;
            }
            openUserForm(sel);
        });
        view.toggleActivoBtn.setOnAction(e -> runAsync(this::doToggleActivo));
        view.eliminarBtn.setOnAction(e -> runAsync(this::doEliminar));
        view.biometriaBtn.setOnAction(e -> openBiometricDialog());

        // Botón Registro Biométrico con diálogo de selección
        view.registrarBioBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("⚠ Seleccione un usuario");
                return;
            }

            String tipo = view.showBiometricRegistrationDialog();
            if (tipo != null) {
                switch (tipo) {
                    case "completo":
                        runAsync(this::doRegistrarCompleto);
                        break;
                    case "huella":
                        runAsync(() -> doRegistrarModalidad(UserApiClient.Modalidad.HUELLA));
                        break;
                    case "facial":
                        runAsync(() -> doRegistrarModalidad(UserApiClient.Modalidad.ROSTRO));
                        break;
                    case "voz":
                        runAsync(() -> doRegistrarModalidad(UserApiClient.Modalidad.VOZ));
                        break;
                }
            }
        });
    }

    private void loadPage(int page) {
        view.status.setText("Cargando...");
        runAsync(() -> {
            try {
                // Normalizar tipo: si es "Todos", enviar vacío para no filtrar
                String tipo = selVal(view.filtroTipo);
                if ("Todos".equalsIgnoreCase(tipo))
                    tipo = "";
                var filters = new UserApiClient.Filters(
                        textVal(view.filtroDocumento), textVal(view.filtroNombre), tipo,
                        view.filtroActivos.isSelected());
                PageResponse<UserApiClient.UsuarioDto> p = api.listPaged(page, pageSize, filters);
                Platform.runLater(() -> {
                    view.setTableData(p.content);
                    view.pagination.setPageCount(Math.max(1, p.totalPages));
                    view.pagination.setCurrentPageIndex(Math.max(0, Math.min(p.number, p.totalPages - 1)));
                    view.updateStats(p.totalElements);
                    view.status.setText("✅ " + p.content.size() + " registros cargados");
                });
            } catch (IOException ex) {
                ex.printStackTrace(); // Imprimir stack trace completo en consola
                Platform.runLater(() -> view.status.setText("❌ Error: " + ex.getMessage()));
            }
        });
    }

    private void openUserForm(UserApiClient.UsuarioDto existing) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.initOwner(stage);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setResizable(true);

        boolean isView = existing != null && view.verBtn.isFocused();
        String title = existing == null ? "➕ Nuevo Usuario"
                : isView ? "👁 Ver Usuario"
                        : "✏ Editar Usuario";
        dlg.setTitle(title);

        DialogPane pane = dlg.getDialogPane();

        // Si es modo Ver, usar la vista simplificada
        if (isView) {
            UserViewCard viewCard = new UserViewCard(existing);
            pane.setContent(viewCard);

            // Agregar ButtonType oculto para permitir cerrar con X
            ButtonType hiddenClose = new ButtonType("", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().setAll(hiddenClose);
            pane.lookupButton(hiddenClose).setVisible(false);
            pane.lookupButton(hiddenClose).setManaged(false);

            // Tamaño compacto para vista
            pane.setMinWidth(550);
            pane.setMaxWidth(650);
            pane.setPrefWidth(600);
        } else {
            // Modo crear/editar con formulario completo
            UserFormViewV2 form = new UserFormViewV2(dto -> runAsync(() -> doSubmit(existing, dto, dlg)));
            form.load(existing);
            pane.setContent(form);

            // Agregar ButtonType oculto para permitir cerrar con X
            ButtonType hiddenClose = new ButtonType("", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().setAll(hiddenClose);
            pane.lookupButton(hiddenClose).setVisible(false);
            pane.lookupButton(hiddenClose).setManaged(false);

            form.getCancelarBtn().setOnAction(e -> dlg.close());

            // Tamaño para formulario
            pane.setMinWidth(650);
            pane.setMaxWidth(850);
            pane.setPrefWidth(750);
        }

        dlg.show();
    }

    private void doSubmit(UserApiClient.UsuarioDto existing, UserApiClient.UsuarioDto dto, Dialog<?> dlg) {
        try {
            if (existing == null)
                api.create(dto);
            else
                api.update(existing.id, dto);
            Platform.runLater(() -> {
                dlg.close();
                loadPage(0);
                refreshBioChips();
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error guardando: " + e.getMessage()));
        }
    }

    private void doDesactivar() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> view.status.setText("Seleccione un usuario"));
            return;
        }
        try {
            api.desactivar(sel.id);
            Platform.runLater(() -> {
                view.status.setText("Usuario desactivado");
                loadPage(view.pagination.getCurrentPageIndex());
                refreshBioChips();
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error desactivando: " + e.getMessage()));
        }
    }

    private void doToggleActivo() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> view.status.setText("Seleccione un usuario"));
            return;
        }
        try {
            if (Boolean.TRUE.equals(sel.activo)) {
                api.desactivar(sel.id);
                Platform.runLater(() -> view.status.setText("Usuario desactivado"));
            } else {
                api.reactivar(sel.id);
                Platform.runLater(() -> view.status.setText("Usuario reactivado"));
            }
            Platform.runLater(() -> {
                loadPage(view.pagination.getCurrentPageIndex());
                refreshBioChips();
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error activando/desactivando: " + e.getMessage()));
        }
    }

    private void doEliminar() {
        // Eliminación lógica reutiliza desactivar por ahora
        doDesactivar();
    }

    private void doRegistrarCompleto() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> view.status.setText("Seleccione un usuario"));
            return;
        }
        try {
            api.enrolar(sel.id, UserApiClient.Modalidad.HUELLA);
            api.enrolar(sel.id, UserApiClient.Modalidad.ROSTRO);
            api.enrolar(sel.id, UserApiClient.Modalidad.VOZ);
            Platform.runLater(() -> {
                view.status.setText("Biometría completa registrada");
                refreshBioChips();
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error registrando biometría: " + e.getMessage()));
        }
    }

    private void doRegistrarModalidad(UserApiClient.Modalidad modalidad) {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> view.status.setText("Seleccione un usuario"));
            return;
        }
        try {
            api.enrolar(sel.id, modalidad);
            Platform.runLater(() -> {
                view.status.setText("Registrado " + modalidad);
                refreshBioChips();
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error registrando " + modalidad + ": " + e.getMessage()));
        }
    }

    private void openBiometricDialog() {
        var sel = view.getSelected();
        if (sel == null) {
            view.status.setText("⚠ Seleccione un usuario");
            return;
        }
        Dialog<Void> dlg = new Dialog<>();
        dlg.initOwner(stage);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setResizable(true);
        dlg.setTitle("🔐 Biometría de " + sel.nombreCompleto);

        BiometricManagementViewV2 bio = new BiometricManagementViewV2();
        bio.setUserId(sel.id);

        // Envolver en ScrollPane para permitir scroll cuando la ventana esté minimizada
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(bio);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        DialogPane pane = dlg.getDialogPane();
        pane.setContent(scrollPane);

        // Agregar ButtonType oculto para permitir cerrar con X
        ButtonType hiddenClose = new ButtonType("", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().setAll(hiddenClose);
        pane.lookupButton(hiddenClose).setVisible(false);
        pane.lookupButton(hiddenClose).setManaged(false);

        // Configurar tamaño responsive y manejable
        pane.setMinWidth(750);
        pane.setMaxWidth(900);
        pane.setPrefWidth(820);
        pane.setMinHeight(500);
        pane.setPrefHeight(580);
        pane.setMaxHeight(700);

        bio.enrolarBtn.setOnAction(e -> runAsync(() -> doEnrolarV2(sel.id, bio)));
        bio.desactivarBtn.setOnAction(e -> runAsync(() -> doDesactivarPlantillaV2(sel.id, bio)));
        bio.registrarWebAuthnBtn.setOnAction(e -> {
            // Lanzar registro WebAuthn para el documento del usuario
            var wapi = new co.cellano.edufeed.desktop.service.WebAuthnApiClient(
                    getBaseUrlFromApi(), getBearerFromApi());
            var webDlg = new co.cellano.edufeed.desktop.access.WebAuthnDialog(
                    stage, wapi,
                    co.cellano.edufeed.desktop.access.WebAuthnDialog.Mode.REGISTRO,
                    sel.documento);
            webDlg.showAndOnSuccess(
                    () -> Platform.runLater(() -> {
                        bio.status.setText("✅ Credencial WebAuthn registrada exitosamente");
                        bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
                    }));
        });

        dlg.show();
        // cargar al abrir
        runAsync(() -> doCargarPlantillasV2(sel.id, bio));
    }

    private void doCargarPlantillas(String userId, BiometricManagementView bio) {
        try {
            List<UserApiClient.PlantillaBiometricaDto> l = api.listarBiometrias(userId);
            Platform.runLater(() -> bio.table.getItems().setAll(l));
        } catch (IOException e) {
            Platform.runLater(() -> bio.status.setText("Error cargando: " + e.getMessage()));
        }
    }

    private void doEnrolar(String userId, BiometricManagementView bio) {
        try {
            var mod = bio.modalidad.getValue();
            api.enrolar(userId, mod);
            doCargarPlantillas(userId, bio);
            Platform.runLater(() -> bio.status.setText("Enrolado " + mod));
        } catch (IOException e) {
            Platform.runLater(() -> bio.status.setText("Error enrolando: " + e.getMessage()));
        }
    }

    private void doDesactivarPlantilla(String userId, BiometricManagementView bio) {
        var sel = bio.table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Platform.runLater(() -> bio.status.setText("Seleccione una plantilla"));
            return;
        }
        try {
            api.desactivarBiometria(userId, sel.id);
            doCargarPlantillas(userId, bio);
            Platform.runLater(() -> bio.status.setText("Plantilla desactivada"));
        } catch (IOException e) {
            Platform.runLater(() -> bio.status.setText("Error desactivando: " + e.getMessage()));
        }
    }

    // Versiones V2 para la vista modernizada
    private void doCargarPlantillasV2(String userId, BiometricManagementViewV2 bio) {
        try {
            List<UserApiClient.PlantillaBiometricaDto> l = api.listarBiometrias(userId);
            Platform.runLater(() -> {
                bio.table.getItems().setAll(l);
                bio.status.setText(String.format("✅ %d plantilla%s cargada%s", l.size(),
                        l.size() == 1 ? "" : "s", l.size() == 1 ? "" : "s"));
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                bio.status.setText("❌ Error cargando: " + e.getMessage());
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-error;");
            });
        }
    }

    private void doEnrolarV2(String userId, BiometricManagementViewV2 bio) {
        try {
            var mod = bio.modalidad.getValue();
            Platform.runLater(() -> {
                bio.status.setText("⏳ Enrolando " + mod + "...");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-secondary;");
            });
            api.enrolar(userId, mod);
            doCargarPlantillasV2(userId, bio);
            Platform.runLater(() -> {
                bio.status.setText("✅ " + mod + " enrolado exitosamente");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                bio.status.setText("❌ Error enrolando: " + e.getMessage());
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-error;");
            });
        }
    }

    private void doDesactivarPlantillaV2(String userId, BiometricManagementViewV2 bio) {
        var sel = bio.table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Platform.runLater(() -> {
                bio.status.setText("⚠ Seleccione una plantilla para desactivar");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-warning;");
            });
            return;
        }
        try {
            api.desactivarBiometria(userId, sel.id);
            doCargarPlantillasV2(userId, bio);
            Platform.runLater(() -> {
                bio.status.setText("✅ Plantilla desactivada exitosamente");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                bio.status.setText("❌ Error desactivando: " + e.getMessage());
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-error;");
            });
        }
    }

    private void refreshBioChips() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> view.bioChips.getChildren().clear());
            return;
        }
        try {
            List<UserApiClient.PlantillaBiometricaDto> l = api.listarBiometrias(sel.id);
            Platform.runLater(() -> {
                view.bioChips.getChildren().clear();
                addBadge("Huella", hasActive(l, "HUELLA"));
                addBadge("FaceID", hasActive(l, "ROSTRO"));
                addBadge("Voz", hasActive(l, "VOZ"));
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.status.setText("Error biometría: " + e.getMessage()));
        }
    }

    private static boolean hasActive(List<UserApiClient.PlantillaBiometricaDto> l, String modalidad) {
        return l != null
                && l.stream().anyMatch(p -> modalidad.equalsIgnoreCase(p.modalidad) && Boolean.TRUE.equals(p.activo));
    }

    private void addBadge(String label, boolean ok) {
        Label chip = new Label(label);
        chip.getStyleClass().add("app-badge");
        chip.getStyleClass().add(ok ? "app-badge--success" : "app-badge--warning");
        view.bioChips.getChildren().add(chip);
    }

    private static String textVal(javafx.scene.control.TextField tf) {
        return tf.getText() != null ? tf.getText().trim() : "";
    }

    private static String selVal(javafx.scene.control.ComboBox<String> cb) {
        return cb.getValue() != null ? cb.getValue().trim() : "";
    }

    private void runAsync(Runnable r) {
        new Thread(r, "admin").start();
    }

    private String getBaseUrlFromApi() {
        try {
            var f = UserApiClient.class.getDeclaredField("baseUrl");
            f.setAccessible(true);
            return (String) f.get(api);
        } catch (Exception e) {
            return "http://localhost:8080";
        }
    }

    private String getBearerFromApi() {
        try {
            var f = UserApiClient.class.getDeclaredField("bearer");
            f.setAccessible(true);
            return (String) f.get(api);
        } catch (Exception e) {
            return null;
        }
    }
}
