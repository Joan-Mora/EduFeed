package co.cellano.edufeed.desktop.modules;

import co.cellano.edufeed.desktop.admin.BiometricManagementViewV2;
import co.cellano.edufeed.desktop.admin.UserFormViewV2;
import co.cellano.edufeed.desktop.admin.UserManagementViewV2;
import co.cellano.edufeed.desktop.admin.UserViewCard;
import co.cellano.edufeed.desktop.service.UserApiClient;
import co.cellano.edufeed.desktop.service.UserApiClient.PageResponse;
import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;

/**
 * Módulo embebible de Administración de Usuarios MODERNIZADO.
 * Permite CRUD de usuarios, gestión de biometría local y WebAuthn.
 * Usa las vistas V2 con diseño card-based y UX mejorada.
 */
public class UserManagementModule {
    private final UserManagementViewV2 view = new UserManagementViewV2();
    private final UserApiClient api;
    private final String baseUrl;
    private final String bearer;
    private final int pageSize = 20;

    public UserManagementModule(String baseUrl, String bearer) {
        this.baseUrl = baseUrl;
        this.bearer = bearer;
        this.api = new UserApiClient(baseUrl, bearer);
        wire();
    }

    /**
     * Retorna la vista principal (Node embebible)
     */
    public Node getView() {
        loadPage(0);
        AnimationUtils.fadeIn(view, AnimationUtils.FAST);
        return view;
    }

    private void wire() {
        view.buscarBtn.setOnAction(e -> loadPage(0));
        view.limpiarBtn.setOnAction(e -> {
            view.filtroDocumento.clear();
            view.filtroNombre.clear();
            view.filtroTipo.setValue("");
            view.filtroActivos.setSelected(false);
            loadPage(0);
        });
        view.pagination.currentPageIndexProperty().addListener((obs, ov, nv) -> loadPage(nv.intValue()));

        view.crearBtn.setOnAction(e -> openUserForm(null));
        view.verBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
                return;
            }
            openUserForm(sel); // reutilizamos el formulario en modo lectura por ahora
        });
        view.editarBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
                return;
            }
            openUserForm(sel);
        });
        view.toggleActivoBtn.setOnAction(e -> runAsync(this::doToggleActivo));
        view.eliminarBtn.setOnAction(e -> runAsync(this::doEliminar));
        view.biometriaBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
                return;
            }
            openBiometricDialog(sel);
        });

        // Botón Registro Biométrico con diálogo de selección
        view.registrarBioBtn.setOnAction(e -> {
            var sel = view.getSelected();
            if (sel == null) {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
                return;
            }

            String tipo = view.showBiometricRegistrationDialog();
            if (tipo != null) {
                switch (tipo) {
                    case "completo":
                        runAsync(() -> doRegistrarCompleto());
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
        view.status.setText("🔄 Cargando...");
        runAsync(() -> {
            try {
                // Normalizar tipo: si es "Todos", enviar vacío para no filtrar
                String tipo = selVal(view.filtroTipo);
                if ("Todos".equalsIgnoreCase(tipo))
                    tipo = "";
                var filters = new UserApiClient.Filters(
                        textVal(view.filtroDocumento),
                        textVal(view.filtroNombre),
                        tipo,
                        view.filtroActivos.isSelected());
                PageResponse<UserApiClient.UsuarioDto> p = api.listPaged(page, pageSize, filters);
                Platform.runLater(() -> {
                    view.setTableData(p.content);
                    view.pagination.setPageCount(Math.max(1, p.totalPages));
                    view.pagination.setCurrentPageIndex(Math.max(0, Math.min(p.number, p.totalPages - 1)));
                    view.updateStats(p.totalElements);
                    view.status.setText("✅ " + p.content.size() + " registros cargados");
                    AnimationUtils.fadeIn(view.table, AnimationUtils.MICRO);
                });
            } catch (IOException ex) {
                ex.printStackTrace(); // Imprimir stack trace completo en consola
                Platform.runLater(() -> {
                    view.status.setText("❌ Error: " + ex.getMessage());
                    AnimationUtils.shake(view.status);
                });
            }
        });
    }

    private void openUserForm(UserApiClient.UsuarioDto existing) {
        Dialog<Void> dlg = new Dialog<>();
        Window owner = view.getScene().getWindow();
        dlg.initOwner(owner);
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

        AnimationUtils.fadeIn(pane, AnimationUtils.FAST);
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
                view.status.setText("✓ Usuario guardado");
                AnimationUtils.pulse(view.status);
                loadPage(0);
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                view.status.setText("❌ Error guardando: " + e.getMessage());
                AnimationUtils.shake(view.status);
            });
        }
    }

    private void doDesactivar() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
            });
            return;
        }
        try {
            api.desactivar(sel.id);
            Platform.runLater(() -> {
                view.status.setText("✓ Usuario desactivado");
                AnimationUtils.pulse(view.status);
                loadPage(view.pagination.getCurrentPageIndex());
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                view.status.setText("❌ Error desactivando: " + e.getMessage());
                AnimationUtils.shake(view.status);
            });
        }
    }

    private void doToggleActivo() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
            });
            return;
        }
        try {
            if (Boolean.TRUE.equals(sel.activo)) {
                api.desactivar(sel.id);
                Platform.runLater(() -> view.status.setText("✓ Usuario desactivado"));
            } else {
                api.reactivar(sel.id);
                Platform.runLater(() -> view.status.setText("✓ Usuario reactivado"));
            }
            Platform.runLater(() -> {
                AnimationUtils.pulse(view.status);
                loadPage(view.pagination.getCurrentPageIndex());
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                view.status.setText("❌ Error activando/desactivando: " + e.getMessage());
                AnimationUtils.shake(view.status);
            });
        }
    }

    private void doEliminar() {
        // Por ahora, usamos el mismo endpoint de desactivar como "eliminar lógico"
        doDesactivar();
    }

    private void doRegistrarCompleto() {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
            });
            return;
        }

        // Abrir wizard de registro biométrico completo
        Platform.runLater(() -> {
            try {
                co.cellano.edufeed.desktop.admin.biometric.BiometricRegistrationWizard wizard = new co.cellano.edufeed.desktop.admin.biometric.BiometricRegistrationWizard(
                        sel.id,
                        sel.nombreCompleto,
                        "http://localhost:8080" // TODO: obtener de configuración
                );
                wizard.showAndWait();
                view.status.setText("✓ Proceso de registro biométrico completado");
                AnimationUtils.pulse(view.status);
            } catch (Exception e) {
                view.status.setText("❌ Error abriendo wizard: " + e.getMessage());
                AnimationUtils.shake(view.status);
                e.printStackTrace();
            }
        });
    }

    private void doRegistrarModalidad(UserApiClient.Modalidad modalidad) {
        var sel = view.getSelected();
        if (sel == null) {
            Platform.runLater(() -> {
                view.status.setText("⚠️ Seleccione un usuario");
                AnimationUtils.shake(view.table);
            });
            return;
        }

        // Abrir diálogo con QR para modalidad específica (no fuerza las 3)
        Platform.runLater(() -> {
            try {
                String baseUrl = "http://localhost:8080"; // TODO: obtener de configuración
                String token = java.util.UUID.randomUUID().toString();
                String sessionId = java.util.UUID.randomUUID().toString();
                // type: huella|rostro|voz
                String typeParam = modalidad.name().toLowerCase().replace("huellas", "huella");
                if (modalidad == UserApiClient.Modalidad.ROSTRO)
                    typeParam = "rostro";
                else if (modalidad == UserApiClient.Modalidad.VOZ)
                    typeParam = "voz";

                String url = String.format("%s/api/biometric/register?userId=%s&token=%s&sessionId=%s&type=%s",
                        baseUrl, sel.id, token, sessionId, typeParam);

                showSingleModalityQR(sel.nombreCompleto, url, modalidad.name());

                view.status.setText("✓ QR generado para " + modalidad);
                AnimationUtils.pulse(view.status);
            } catch (Exception e) {
                view.status.setText("❌ Error generando QR: " + e.getMessage());
                AnimationUtils.shake(view.status);
                e.printStackTrace();
            }
        });
    }

    private void showSingleModalityQR(String userName, String url, String modalidad) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.initOwner(view.getScene().getWindow());
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setTitle("Registro de " + modalidad + " - " + userName);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label title = new Label("📱 Escanea el código QR");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Label instr = new Label("Este QR registrará solo " + modalidad + " sin forzar las otras modalidades.");
        instr.setWrapText(true);
        instr.setMaxWidth(350);
        instr.setStyle("-fx-text-fill: gray;");

        Label statusLabel = new Label("⏳ Esperando registro...");
        statusLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");

        ImageView qrView = new ImageView();
        qrView.setFitWidth(300);
        qrView.setFitHeight(300);

        Label urlLabel = new Label("URL: " + url);
        urlLabel.setWrapText(true);
        urlLabel.setMaxWidth(350);
        urlLabel.setStyle("-fx-font-size: 9; -fx-text-fill: gray;");

        content.getChildren().addAll(title, instr, statusLabel, qrView, urlLabel);

        // Generar QR en hilo separado
        new Thread(() -> {
            try {
                java.awt.image.BufferedImage qr = co.cellano.edufeed.desktop.util.QRCodeGenerator.generateQRCode(url,
                        300, 300);
                javafx.scene.image.Image fxImg = javafx.embed.swing.SwingFXUtils.toFXImage(qr, null);
                Platform.runLater(() -> qrView.setImage(fxImg));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Error generando QR: " + e.getMessage());
                    err.showAndWait();
                    dlg.close();
                });
            }
        }).start();

        // Polling para detectar cuando se complete el registro y cerrar automáticamente
        String userId = url.contains("userId=") ? url.split("userId=")[1].split("&")[0] : "";
        String sessionId = url.contains("sessionId=") ? url.split("sessionId=")[1].split("&")[0] : "";
        if (!userId.isEmpty() && !sessionId.isEmpty()) {
            java.util.concurrent.ScheduledExecutorService poller = java.util.concurrent.Executors
                    .newSingleThreadScheduledExecutor();
            poller.scheduleAtFixedRate(() -> {
                try {
                    co.cellano.edufeed.desktop.service.BiometricSessionService sessSvc = new co.cellano.edufeed.desktop.service.BiometricSessionService(
                            baseUrl);
                    var status = sessSvc.getStatus(userId, sessionId);
                    boolean completed = false;
                    if (modalidad.contains("HUELLA") && status.huellaCompletada)
                        completed = true;
                    if (modalidad.contains("ROSTRO") && status.rostroCompletado)
                        completed = true;
                    if (modalidad.contains("VOZ") && status.vozCompletada)
                        completed = true;
                    if (completed) {
                        Platform.runLater(() -> {
                            statusLabel.setText("✅ ¡Registro completado!");
                            statusLabel.setStyle("-fx-font-size: 16; -fx-text-fill: green; -fx-font-weight: bold;");
                        });
                        poller.shutdown();
                        Thread.sleep(1500);
                        Platform.runLater(() -> dlg.close());
                    }
                } catch (Exception ignore) {
                }
            }, 1, 2, java.util.concurrent.TimeUnit.SECONDS);
            dlg.setOnHidden(e -> poller.shutdown());
        }

        DialogPane pane = dlg.getDialogPane();
        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private void openBiometricDialog(UserApiClient.UsuarioDto usuario) {
        Dialog<Void> dlg = new Dialog<>();
        Window owner = view.getScene().getWindow();
        dlg.initOwner(owner);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.setResizable(true);
        dlg.setTitle("🔐 Biometría de " + usuario.nombreCompleto);

        BiometricManagementViewV2 bio = new BiometricManagementViewV2();
        bio.setUserId(usuario.id);

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

        // Conectar botones
        bio.enrolarBtn.setOnAction(e -> runAsync(() -> doEnrolarV2(usuario.id, bio)));
        bio.desactivarBtn.setOnAction(e -> runAsync(() -> doDesactivarPlantillaV2(usuario.id, bio)));
        bio.registrarWebAuthnBtn.setOnAction(e -> {
            var wapi = new co.cellano.edufeed.desktop.service.WebAuthnApiClient(baseUrl, bearer);
            // WebAuthnDialog requiere Stage, cast desde Window
            javafx.stage.Stage ownerStage = (owner instanceof javafx.stage.Stage)
                    ? (javafx.stage.Stage) owner
                    : null;

            var webDlg = new co.cellano.edufeed.desktop.access.WebAuthnDialog(
                    ownerStage,
                    wapi,
                    co.cellano.edufeed.desktop.access.WebAuthnDialog.Mode.REGISTRO,
                    usuario.documento);
            webDlg.showAndOnSuccess(() -> Platform.runLater(() -> {
                bio.status.setText("✅ Credencial WebAuthn registrada exitosamente");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
                AnimationUtils.pulse(bio.status);
            }));
        });

        AnimationUtils.fadeIn(pane, AnimationUtils.FAST);
        dlg.show();

        // Cargar plantillas al abrir
        runAsync(() -> doCargarPlantillasV2(usuario.id, bio));
    }

    // Métodos para gestión biométrica V2
    private void doCargarPlantillasV2(String userId, BiometricManagementViewV2 bio) {
        try {
            List<UserApiClient.PlantillaBiometricaDto> l = api.listarBiometrias(userId);
            Platform.runLater(() -> {
                bio.table.getItems().setAll(l);
                bio.status.setText(String.format("✅ %d plantilla%s cargada%s", l.size(),
                        l.size() == 1 ? "" : "s", l.size() == 1 ? "" : "s"));
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
                AnimationUtils.fadeIn(bio.table, AnimationUtils.MICRO);
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                bio.status.setText("❌ Error cargando: " + e.getMessage());
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-error;");
                AnimationUtils.shake(bio.status);
            });
        }
    }

    private void doEnrolarV2(String userId, BiometricManagementViewV2 bio) {
        try {
            var mod = bio.modalidad.getValue();
            if (mod == null) {
                Platform.runLater(() -> {
                    bio.status.setText("⚠️ Seleccione una modalidad");
                    bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-warning;");
                    AnimationUtils.shake(bio.modalidad);
                });
                return;
            }

            Platform.runLater(() -> {
                bio.status.setText("⏳ Enrolando " + mod + "...");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-secondary;");
            });

            api.enrolar(userId, mod);
            doCargarPlantillasV2(userId, bio);

            Platform.runLater(() -> {
                bio.status.setText("✅ " + mod + " enrolado exitosamente");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
                AnimationUtils.pulse(bio.status);
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                bio.status.setText("❌ Error enrolando: " + e.getMessage());
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-error;");
                AnimationUtils.shake(bio.status);
            });
        }
    }

    private void doDesactivarPlantillaV2(String userId, BiometricManagementViewV2 bio) {
        var sel = bio.table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Platform.runLater(() -> {
                bio.status.setText("⚠️ Seleccione una plantilla para desactivar");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-warning;");
                AnimationUtils.shake(bio.table);
            });
            return;
        }
        try {
            api.desactivarBiometria(userId, sel.id);
            doCargarPlantillasV2(userId, bio);
            Platform.runLater(() -> {
                bio.status.setText("✅ Plantilla desactivada exitosamente");
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-success;");
                AnimationUtils.pulse(bio.status);
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                bio.status.setText("❌ Error desactivando: " + e.getMessage());
                bio.status.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-error;");
                AnimationUtils.shake(bio.status);
            });
        }
    }

    private static String textVal(javafx.scene.control.TextField tf) {
        return tf.getText() != null ? tf.getText().trim() : "";
    }

    private static String selVal(javafx.scene.control.ComboBox<String> cb) {
        return cb.getValue() != null ? cb.getValue().trim() : "";
    }

    private void runAsync(Runnable r) {
        new Thread(r, "user-mgmt").start();
    }
}
