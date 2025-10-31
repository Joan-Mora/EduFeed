package co.cellano.edufeed.desktop.access;

import co.cellano.edufeed.desktop.biometric.LocalBiometricTestService;
import co.cellano.edufeed.desktop.service.AccessApiClient;
import java.io.IOException;
import java.util.UUID;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AccessCheckController {
    private final Stage stage;
    private final AccessApiClient api;

    public AccessCheckController(Stage stage, AccessApiClient api) {
        this.stage = stage;
        this.api = api;
    }

    public void start() {
    BiometricCaptureView view = new BiometricCaptureView(this::onCapture);
    String faceSource = System.getenv().getOrDefault("DESKTOP_FACE_SOURCE", "camera:0");
    int voiceSec = Integer.parseInt(System.getenv().getOrDefault("DESKTOP_VOICE_SECONDS", "2"));
    var tester = new LocalBiometricTestService(faceSource, voiceSec);
    view.bindHardwareTest(tester::testHardware);
        stage.setScene(new Scene(view, 720, 420));
        stage.setTitle("EduFeed — Punto de acceso");
        stage.show();
    }

    private void onCapture(UUID usuarioId, String modalidad) {
        // detener previews de la vista actual antes de cambiar
        var sceneRoot = stage.getScene() != null ? stage.getScene().getRoot() : null;
        if (sceneRoot instanceof BiometricCaptureView bcv) {
            bcv.stopPreviews();
        }
        AccessCheckView loading = new AccessCheckView();
        loading.showApproved("Capturando " + modalidad + "…");
        stage.setScene(new Scene(loading, 720, 420));

        new Thread(() -> {
            try {
                var res = api.checkAccess(usuarioId, modalidad);
                Platform.runLater(() -> showResult(res));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    AccessCheckView errorV = new AccessCheckView();
                    errorV.showDenied("Error llamando API: " + e.getMessage());
                    stage.setScene(new Scene(errorV, 720, 420));
                });
            }
        }).start();
    }

    private void showResult(AccessApiClient.AccesoCheckResponseDto res) {
        if (res.permitido != null && res.permitido) {
            AccessCheckView ok = new AccessCheckView();
            var nombre = res.usuario != null ? res.usuario.nombreCompleto : "Usuario";
            ok.showApproved("Acceso permitido a " + nombre);
            stage.setScene(new Scene(ok, 720, 420));
        } else {
            var root = new BorderPane();
            AccessCheckView ko = new AccessCheckView();
            ko.showDenied(res.motivo != null ? res.motivo : "Acceso denegado");

            CashierRedirectView cash = new CashierRedirectView();
            if (res.orientacionCaja != null) {
                cash.setData(res.orientacionCaja.mensaje, res.orientacionCaja.ubicacionCaja,
                        res.orientacionCaja.horarioAtencion, res.orientacionCaja.referencia, res.orientacionCaja.codigoQR);
            }
            root.setTop(ko);
            root.setCenter(cash);
            stage.setScene(new Scene(root, 720, 520));
        }
    }
}
