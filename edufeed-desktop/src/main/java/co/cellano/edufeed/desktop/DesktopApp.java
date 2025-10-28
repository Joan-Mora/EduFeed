package co.cellano.edufeed.desktop;

import co.cellano.edufeed.desktop.access.AccessCheckController;
import co.cellano.edufeed.desktop.service.AccessApiClient;
import java.util.Arrays;
import java.util.Optional;
import javafx.application.Application;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;

public class DesktopApp extends Application {
    @Override
    public void start(Stage stage) {
        var baseUrl = System.getenv().getOrDefault("BACKEND_BASE_URL", "http://localhost:8080");
        var token = System.getenv().getOrDefault("BACKEND_BEARER_TOKEN", "");
        if (token == null || token.isBlank()) {
            // flujo de login ligero
            new co.cellano.edufeed.desktop.access.LoginController(stage, baseUrl).start(accessToken -> {
                startWithToken(stage, baseUrl, accessToken);
            });
        } else {
            startWithToken(stage, baseUrl, token);
        }
    }

    private void startWithToken(Stage stage, String baseUrl, String token) {
        // Permitir elegir módulo por diálogo o por variable de entorno DESKTOP_DEFAULT_MODULE ("acceso"|"caja")
        String preferred = Optional.ofNullable(System.getenv("DESKTOP_DEFAULT_MODULE"))
                .map(String::toLowerCase).orElse("");
        if (preferred.isBlank()) {
            ChoiceDialog<String> dlg = new ChoiceDialog<>("Caja", Arrays.asList("Caja", "Acceso", "Administración", "Reportes"));
            dlg.setTitle("EduFeed — Módulos");
            dlg.setHeaderText("Seleccione un módulo para iniciar");
            dlg.setContentText("Módulo:");
            preferred = dlg.showAndWait().orElse("Acceso").toLowerCase();
        }

        switch (preferred) {
            case "caja" -> new co.cellano.edufeed.desktop.cashier.CashierController(stage, baseUrl, token).start();
            case "acceso" -> {
                var api = new AccessApiClient(baseUrl, token);
                new AccessCheckController(stage, api).start();
            }
            case "administración", "administracion" -> new co.cellano.edufeed.desktop.admin.UserManagementController(stage, baseUrl, token).start();
            case "reportes" -> new co.cellano.edufeed.desktop.reports.ReportsController(stage, baseUrl, token).start();
            default -> {
                var api = new AccessApiClient(baseUrl, token);
                new AccessCheckController(stage, api).start();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
