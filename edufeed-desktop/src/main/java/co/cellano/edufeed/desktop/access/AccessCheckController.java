package co.cellano.edufeed.desktop.access;

import co.cellano.edufeed.desktop.biometric.LocalBiometricTestService;
import co.cellano.edufeed.desktop.service.AccessApiClient;
import co.cellano.edufeed.desktop.service.WebAuthnApiClient;
import java.io.IOException;
import java.util.UUID;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AccessCheckController {
    private final Stage stage;
    private final AccessApiClient api;
    private final Runnable onChangeModule;
    private javafx.scene.layout.BorderPane root;

    public AccessCheckController(Stage stage, AccessApiClient api) {
        this(stage, api, null);
    }

    public AccessCheckController(Stage stage, AccessApiClient api, Runnable onChangeModule) {
        this.stage = stage;
        this.api = api;
        this.onChangeModule = onChangeModule;
    }

    public void start() {
        BiometricCaptureView view = new BiometricCaptureView(this::onCapture);
        String faceSource = System.getenv().getOrDefault("DESKTOP_FACE_SOURCE", "camera:0");
        int voiceSec = Integer.parseInt(System.getenv().getOrDefault("DESKTOP_VOICE_SECONDS", "2"));
        var tester = new LocalBiometricTestService(faceSource, voiceSec);
        view.bindHardwareTest(tester::testHardware);
        // Conectar lanzador WebAuthn al diálogo modal y, al completar con éxito,
        // ejecutar captura HUELLAS
        view.setWebAuthnLauncher(uid -> {
            WebAuthnApiClient w = new WebAuthnApiClient(api.getBaseUrl(), api.getBearerToken());
            WebAuthnDialog dlg = new WebAuthnDialog(stage, w);
            dlg.showAndOnSuccess(() -> onCapture(uid, "HUELLA"));
        });
        root = new javafx.scene.layout.BorderPane(view);
        root.setTop(new co.cellano.edufeed.desktop.ui.NavBar("EduFeed — Punto de acceso", onChangeModule));
        Scene scene = new Scene(root, 720, 460);
        co.cellano.edufeed.desktop.theme.ThemeService.getInstance().register(scene);
        stage.setScene(scene);
        stage.setTitle("EduFeed — Punto de acceso");

        // Centrar ventana
        co.cellano.edufeed.desktop.util.StageUtils.centerWindow(stage, stage.getWidth(), stage.getHeight());

        stage.show();
        co.cellano.edufeed.desktop.util.AnimationUtils.fadeIn(root);
    }

    private void onCapture(UUID usuarioId, String modalidad) {
        // detener previews de la vista actual antes de cambiar
        var centerNode = root != null ? root.getCenter() : null;
        if (centerNode instanceof BiometricCaptureView bcv) {
            bcv.stopPreviews();
        }
        AccessCheckView loading = new AccessCheckView();
        loading.showApproved("Capturando " + modalidad + "…");
        if (root != null) {
            root.setCenter(loading);
            co.cellano.edufeed.desktop.util.AnimationUtils.fadeIn(loading);
        } else
            stage.setScene(new Scene(loading, 720, 460));

        new Thread(() -> {
            try {
                var res = api.checkAccess(usuarioId, modalidad);
                Platform.runLater(() -> showResult(res));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    AccessCheckView errorV = new AccessCheckView();
                    errorV.showDenied("Error llamando API: " + e.getMessage());
                    if (root != null)
                        root.setCenter(errorV);
                    else
                        stage.setScene(new Scene(errorV, 720, 460));
                });
            }
        }).start();
    }

    private void showResult(AccessApiClient.AccesoCheckResponseDto res) {
        if (res.permitido != null && res.permitido) {
            AccessCheckView ok = new AccessCheckView();
            var nombre = res.usuario != null ? res.usuario.nombreCompleto : "Usuario";
            ok.showApproved("Acceso permitido a " + nombre);
            if (root != null) {
                root.setCenter(ok);
                co.cellano.edufeed.desktop.util.AnimationUtils.fadeIn(ok);
            } else
                stage.setScene(new Scene(ok, 720, 460));
        } else {
            AccessCheckView ko = new AccessCheckView();
            ko.showDenied(res.motivo != null ? res.motivo : "Acceso denegado");

            CashierRedirectView cash = new CashierRedirectView();
            if (res.orientacionCaja != null) {
                cash.setData(res.orientacionCaja.mensaje, res.orientacionCaja.ubicacionCaja,
                        res.orientacionCaja.horarioAtencion, res.orientacionCaja.referencia,
                        res.orientacionCaja.codigoQR);
            }
            var wrap = new BorderPane();
            wrap.setTop(ko);
            wrap.setCenter(cash);
            if (this.root != null) {
                this.root.setCenter(wrap);
                co.cellano.edufeed.desktop.util.AnimationUtils.fadeIn(wrap);
            } else
                stage.setScene(new Scene(wrap, 720, 540));
        }
    }
}
