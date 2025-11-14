package co.cellano.edufeed.desktop.access;

import co.cellano.edufeed.desktop.service.AuthApiClient;
import javafx.application.Platform;
import javafx.stage.Stage;

public class LoginController {
    private final Stage stage;
    private final String baseUrl;
    private LoginView view;

    public interface OnLoggedIn {
        void startAccess(String bearerToken);
    }

    public LoginController(Stage stage, String baseUrl) {
        this.stage = stage;
        this.baseUrl = baseUrl;
    }

    public void start(OnLoggedIn callback) {
        view = new LoginView((user, pass) -> doLogin(user, pass, callback));
        javafx.scene.Scene scene = new javafx.scene.Scene(view, 1200, 740);
        co.cellano.edufeed.desktop.theme.ThemeService.getInstance().register(scene);
        stage.setScene(scene);
        stage.setTitle("EduFeed — Inicio de sesión");
        // Modo ventana centrado
        co.cellano.edufeed.desktop.util.StageUtils.centerWindow(stage, 1200, 740);
        stage.show();
    }

    private void doLogin(String user, String pass, OnLoggedIn callback) {
        if (view != null)
            view.showLoading();
        new Thread(() -> {
            try {
                var auth = new AuthApiClient(baseUrl);
                var tokens = auth.login(user, pass);
                Platform.runLater(() -> callback.startAccess(tokens.accessToken()));
            } catch (java.io.IOException e) {
                // Error de red o autenticación - mostrar mensaje del servidor
                String errorMessage = e.getMessage();
                Platform.runLater(() -> {
                    if (view != null)
                        view.showError(errorMessage != null ? errorMessage : "Error de conexión con el servidor");
                });
            } catch (Exception e) {
                // Error inesperado
                Platform.runLater(() -> {
                    if (view != null)
                        view.showError("Error inesperado. Por favor intente nuevamente");
                });
            }
        }, "login").start();
    }
}
