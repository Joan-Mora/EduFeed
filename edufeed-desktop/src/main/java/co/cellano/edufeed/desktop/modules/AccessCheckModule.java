package co.cellano.edufeed.desktop.modules;

import co.cellano.edufeed.desktop.access.AccessCheckView;
import co.cellano.edufeed.desktop.access.BiometricCaptureView;
import co.cellano.edufeed.desktop.access.CashierRedirectView;
import co.cellano.edufeed.desktop.access.WebAuthnDialog;
import co.cellano.edufeed.desktop.biometric.LocalBiometricTestService;
import co.cellano.edufeed.desktop.service.AccessApiClient;
import co.cellano.edufeed.desktop.service.WebAuthnApiClient;
import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

import java.io.IOException;
import java.util.UUID;

/**
 * Módulo embebible de Control de Acceso.
 * Permite verificación biométrica local (rostro, voz, huella) y WebAuthn.
 */
public class AccessCheckModule {
    private final BorderPane root = new BorderPane();
    private final AccessApiClient api;
    private final String baseUrl;
    private final String bearerToken;

    public AccessCheckModule(String baseUrl, String bearerToken) {
        this.baseUrl = baseUrl;
        this.bearerToken = bearerToken;
        this.api = new AccessApiClient(baseUrl, bearerToken);
        initializeView();
    }

    /**
     * Retorna la vista principal (Node embebible)
     */
    public Node getView() {
        AnimationUtils.fadeIn(root, AnimationUtils.FAST);
        return root;
    }

    private void initializeView() {
        BiometricCaptureView captureView = new BiometricCaptureView(this::onCapture);

        // Configurar hardware biométrico local
        String faceSource = System.getenv().getOrDefault("DESKTOP_FACE_SOURCE", "camera:0");
        int voiceSec = Integer.parseInt(System.getenv().getOrDefault("DESKTOP_VOICE_SECONDS", "2"));
        var tester = new LocalBiometricTestService(faceSource, voiceSec);
        captureView.bindHardwareTest(tester::testHardware);

        // Configurar lanzador WebAuthn
        captureView.setWebAuthnLauncher(uid -> {
            WebAuthnApiClient wapi = new WebAuthnApiClient(baseUrl, bearerToken);
            Window owner = root.getScene() != null ? root.getScene().getWindow() : null;

            // WebAuthnDialog requiere Stage
            javafx.stage.Stage ownerStage = (owner instanceof javafx.stage.Stage)
                    ? (javafx.stage.Stage) owner
                    : null;

            WebAuthnDialog dlg = new WebAuthnDialog(ownerStage, wapi);
            dlg.showAndOnSuccess(() -> onCapture(uid, "HUELLA"));
        });

        root.setCenter(captureView);
    }

    private void onCapture(UUID usuarioId, String modalidad) {
        // Detener previews antes de cambiar vista
        var centerNode = root.getCenter();
        if (centerNode instanceof BiometricCaptureView bcv) {
            bcv.stopPreviews();
        }

        // Mostrar loading
        AccessCheckView loading = new AccessCheckView();
        loading.showApproved("🔄 Capturando " + modalidad + "…");
        root.setCenter(loading);
        AnimationUtils.fadeIn(loading, AnimationUtils.FAST);

        // Llamada API asíncrona
        new Thread(() -> {
            try {
                var res = api.checkAccess(usuarioId, modalidad);
                Platform.runLater(() -> showResult(res));
            } catch (IOException e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }, "access-check").start();
    }

    private void showResult(AccessApiClient.AccesoCheckResponseDto res) {
        if (res.permitido != null && res.permitido) {
            // Acceso PERMITIDO
            AccessCheckView okView = new AccessCheckView();
            var nombre = res.usuario != null ? res.usuario.nombreCompleto : "Usuario";
            okView.showApproved("✅ Acceso permitido a " + nombre);

            root.setCenter(okView);
            AnimationUtils.fadeIn(okView, AnimationUtils.NORMAL);
            AnimationUtils.pulse(okView);

            // Auto-reiniciar después de 3 segundos
            Platform.runLater(() -> {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(3));
                pause.setOnFinished(e -> resetToCapture());
                pause.play();
            });
        } else {
            // Acceso DENEGADO - mostrar orientación a caja
            AccessCheckView koView = new AccessCheckView();
            koView.showDenied(res.motivo != null ? res.motivo : "❌ Acceso denegado");

            CashierRedirectView cashRedirect = new CashierRedirectView();
            if (res.orientacionCaja != null) {
                cashRedirect.setData(
                        res.orientacionCaja.mensaje,
                        res.orientacionCaja.ubicacionCaja,
                        res.orientacionCaja.horarioAtencion,
                        res.orientacionCaja.referencia,
                        res.orientacionCaja.codigoQR);
            }

            var wrapper = new BorderPane();
            wrapper.setTop(koView);
            wrapper.setCenter(cashRedirect);

            root.setCenter(wrapper);
            AnimationUtils.fadeIn(wrapper, AnimationUtils.NORMAL);
            AnimationUtils.shake(koView);

            // Auto-reiniciar después de 5 segundos
            Platform.runLater(() -> {
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(5));
                pause.setOnFinished(e -> resetToCapture());
                pause.play();
            });
        }
    }

    private void showError(String errorMessage) {
        AccessCheckView errorView = new AccessCheckView();
        errorView.showDenied("❌ Error llamando API: " + errorMessage);

        root.setCenter(errorView);
        AnimationUtils.fadeIn(errorView, AnimationUtils.FAST);
        AnimationUtils.shake(errorView);

        // Auto-reiniciar después de 4 segundos
        Platform.runLater(() -> {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(4));
            pause.setOnFinished(e -> resetToCapture());
            pause.play();
        });
    }

    private void resetToCapture() {
        // Reiniciar a vista de captura
        initializeView();
        AnimationUtils.slideInLeft(root.getCenter(), AnimationUtils.NORMAL);
    }
}
