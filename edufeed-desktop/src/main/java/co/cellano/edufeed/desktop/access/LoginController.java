package co.cellano.edufeed.desktop.access;

import co.cellano.edufeed.desktop.service.AuthApiClient;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    private final Stage stage;
    private final String baseUrl;

    public interface OnLoggedIn { void startAccess(String bearerToken); }

    public LoginController(Stage stage, String baseUrl) {
        this.stage = stage;
        this.baseUrl = baseUrl;
    }

    public void start(OnLoggedIn callback) {
        LoginView view = new LoginView((user, pass) -> doLogin(user, pass, callback));
        stage.setScene(new Scene(view, 520, 240));
        stage.setTitle("EduFeed — Login");
        stage.show();
    }

    private void doLogin(String user, String pass, OnLoggedIn callback) {
        new Thread(() -> {
            try {
                var auth = new AuthApiClient(baseUrl);
                var tokens = auth.login(user, pass);
                String bearer = tokens.tokenType()+" "+tokens.accessToken();
                Platform.runLater(() -> callback.startAccess(tokens.accessToken()));
            } catch (Exception e) {
                Platform.runLater(() -> System.err.println("Error en login: "+e.getMessage()));
            }
        }).start();
    }
}
