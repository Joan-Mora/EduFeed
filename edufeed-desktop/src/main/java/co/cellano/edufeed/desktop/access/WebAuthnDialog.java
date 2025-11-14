package co.cellano.edufeed.desktop.access;

import co.cellano.edufeed.desktop.service.WebAuthnApiClient;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Diálogo modal para iniciar WebAuthn (registro o autenticación) y hacer
 * polling hasta completar.
 */
public class WebAuthnDialog {
    private final Stage owner;
    private final WebAuthnApiClient api;
    private ScheduledExecutorService poller;

    public enum Mode {
        AUTENTICACION, REGISTRO
    }

    private final Mode mode;
    private final String presetDocumento;

    public WebAuthnDialog(Stage owner, WebAuthnApiClient api) {
        this(owner, api, Mode.AUTENTICACION, null);
    }

    public WebAuthnDialog(Stage owner, WebAuthnApiClient api, Mode mode, String presetDocumento) {
        this.owner = owner;
        this.api = api;
        this.mode = mode != null ? mode : Mode.AUTENTICACION;
        this.presetDocumento = presetDocumento;
    }

    public void showAndOnSuccess(Runnable onSuccess) {
        Stage dlg = new Stage();
        dlg.initOwner(owner);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(mode == Mode.REGISTRO ? "Registro WebAuthn (teléfono)" : "Autenticación por teléfono (WebAuthn)");

        TextField documento = new TextField();
        documento.setPromptText("Documento del usuario (ej: 123456)");
        Button iniciar = new Button("Iniciar");
        Label estado = new Label("Ingrese el documento y presione Iniciar.");
        Hyperlink link = new Hyperlink();
        link.setVisible(false);
        link.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(link.getText()));
            } catch (Exception ignore) {
            }
        });

        Button copyBtn = new Button("Copiar enlace");
        copyBtn.setVisible(false);
        copyBtn.setOnAction(e -> {
            String url = link.getText();
            if (url != null && !url.isBlank()) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(url);
                Clipboard.getSystemClipboard().setContent(cc);
                estado.setText("Enlace copiado al portapapeles");
            }
        });

        VBox box = new VBox(10,
                new Label(mode == Mode.REGISTRO ? "Registre su teléfono como credencial WebAuthn"
                        : "Use su teléfono como lector de huella"),
                new HBox(8, new Label("Documento:"), documento),
                iniciar,
                estado,
                new HBox(8, link, copyBtn));
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(12));
        dlg.setScene(new Scene(box, 520, 200));

        if (presetDocumento != null && !presetDocumento.isBlank()) {
            documento.setText(presetDocumento);
            documento.setDisable(true);
        }

        iniciar.setOnAction(e -> {
            iniciar.setDisable(true);
            new Thread(() -> {
                try {
                    var doc = documento.getText().trim();
                    WebAuthnApiClient.IniciarResponse resp = (mode == Mode.REGISTRO)
                            ? api.iniciarRegistro(doc)
                            : api.iniciarAutenticacion(doc);
                    UUID sesionId = UUID.fromString(resp.sesionId);
                    var qr = api.obtenerQr(sesionId);
                    Platform.runLater(() -> {
                        link.setText(qr.url);
                        link.setVisible(true);
                        copyBtn.setVisible(true);
                        estado.setText(mode == Mode.REGISTRO
                                ? "Escanee el código/link desde su teléfono para registrar la credencial…"
                                : "Escanee el código/link desde su teléfono…");
                    });
                    // Polling cada 2s
                    poller = Executors.newSingleThreadScheduledExecutor(r -> {
                        var t = new Thread(r, "webauthn-poller");
                        t.setDaemon(true);
                        return t;
                    });
                    poller.scheduleAtFixedRate(() -> {
                        try {
                            var st = api.obtenerEstado(sesionId);
                            Platform.runLater(() -> estado
                                    .setText("Estado: " + st.estado + (st.mensaje != null ? " — " + st.mensaje : "")));
                            if ("COMPLETADA".equalsIgnoreCase(st.estado)) {
                                shutdownPoller();
                                Platform.runLater(() -> {
                                    dlg.close();
                                    if (Boolean.TRUE.equals(st.exito) && onSuccess != null)
                                        onSuccess.run();
                                });
                            } else if ("EXPIRADA".equalsIgnoreCase(st.estado)
                                    || "FALLIDA".equalsIgnoreCase(st.estado)) {
                                shutdownPoller();
                            }
                        } catch (Exception ex) {
                            shutdownPoller();
                            Platform.runLater(() -> estado.setText("Error: " + ex.getMessage()));
                        }
                    }, 0, 2, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        estado.setText("Error iniciando WebAuthn: " + ex.getMessage());
                        iniciar.setDisable(false);
                    });
                }
            }).start();
        });

        dlg.setOnCloseRequest(e -> shutdownPoller());
        dlg.showAndWait();
    }

    private void shutdownPoller() {
        if (poller != null) {
            poller.shutdownNow();
            poller = null;
        }
    }
}
