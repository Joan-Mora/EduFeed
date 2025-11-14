package co.cellano.edufeed.desktop;

import co.cellano.edufeed.desktop.access.AccessCheckController;
import co.cellano.edufeed.desktop.service.AccessApiClient;
import co.cellano.edufeed.desktop.util.JwtUtils;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class DesktopApp extends Application {
    @Override
    public void start(Stage stage) {
        // Asegurar pantalla completa por defecto desde el arranque
        stage.setMaximized(true);
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
        // Mantener pantalla completa como valor por defecto al continuar post-login
        stage.setMaximized(true);
        // Decodificar roles del JWT para filtrar módulos visibles
        Set<String> roles = JwtUtils.extractRoles(token);
        String username = co.cellano.edufeed.desktop.util.JwtUtils.extractUsername(token);

        // Poblar SessionContext para que la UI pueda mostrar usuario y hacer logout
        co.cellano.edufeed.desktop.session.SessionContext.baseUrl = baseUrl;
        co.cellano.edufeed.desktop.session.SessionContext.token = token;
        co.cellano.edufeed.desktop.session.SessionContext.username = username != null ? username : "";
        co.cellano.edufeed.desktop.session.SessionContext.roles = roles;
        co.cellano.edufeed.desktop.session.SessionContext.onLogout = () -> {
            // Volver al login limpiando el contexto
            co.cellano.edufeed.desktop.session.SessionContext.baseUrl = null;
            co.cellano.edufeed.desktop.session.SessionContext.token = null;
            co.cellano.edufeed.desktop.session.SessionContext.username = null;
            co.cellano.edufeed.desktop.session.SessionContext.roles = null;
            // Lanzar login nuevamente
            new co.cellano.edufeed.desktop.access.LoginController(stage, baseUrl).start(accessToken -> {
                startWithToken(stage, baseUrl, accessToken);
            });
        };

        openModulePickerAndLaunch(stage, baseUrl, token, roles, false);
    }

    private void openModulePickerAndLaunch(Stage stage, String baseUrl, String token, Set<String> roles,
            boolean forceDialog) {
        // Capturar el estado de maximización ANTES de mostrar cualquier diálogo
        boolean wasMaximized = stage.isMaximized();

        var allowed = Arrays.asList("Caja", "Acceso", "Administración", "Reportes").stream()
                .filter(m -> isAllowed(m, roles))
                .collect(Collectors.toList());
        if (allowed.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Tu usuario no tiene permisos para ningún módulo (roles: " + roles + ")",
                    ButtonType.OK).showAndWait();
            return;
        }

        String preferred = Optional.ofNullable(System.getenv("DESKTOP_DEFAULT_MODULE"))
                .map(String::toLowerCase).orElse("");
        if (forceDialog || preferred.isBlank() || !isAllowed(capitalize(preferred), roles)) {
            ChoiceDialog<String> dlg = new ChoiceDialog<>(allowed.get(0), allowed);
            dlg.setTitle("EduFeed — Módulos");
            dlg.setHeaderText("Seleccione un módulo para iniciar\nTus roles: " + roles);
            dlg.setContentText("Módulo:");
            preferred = dlg.showAndWait().orElse(allowed.get(0)).toLowerCase();
        }

        Runnable switcher = () -> openModulePickerAndLaunch(stage, baseUrl, token, roles, true);

        // Restaurar maximización justo antes de lanzar el módulo
        if (wasMaximized) {
            stage.setMaximized(true);
        }

        switch (preferred) {
            case "caja" ->
                new co.cellano.edufeed.desktop.cashier.CashierController(stage, baseUrl, token, switcher).start();
            case "acceso" -> {
                var api = new AccessApiClient(baseUrl, token);
                new AccessCheckController(stage, api, switcher).start();
            }
            case "administración", "administracion" ->
                new co.cellano.edufeed.desktop.admin.UserManagementController(stage, baseUrl, token, switcher).start();
            case "reportes" ->
                new co.cellano.edufeed.desktop.reports.ReportsController(stage, baseUrl, token, switcher).start();
            default -> {
                var api = new AccessApiClient(baseUrl, token);
                new AccessCheckController(stage, api, switcher).start();
            }
        }
    }

    private static boolean isAllowed(String module, Set<String> roles) {
        // Spring hasRole('X') equivale a autoridad "ROLE_X"; los tokens llevan "ROLE_*"
        boolean has = roles != null && !roles.isEmpty();
        return switch (module) {
            case "Caja" -> has && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OPERADOR_CAJA"));
            case "Acceso" -> has && (roles.contains("ROLE_OPERADOR_ACCESO") || roles.contains("ROLE_SUPERVISOR")
                    || roles.contains("ROLE_ADMIN"));
            case "Administración" -> has && roles.contains("ROLE_ADMIN");
            case "Reportes" -> has && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_AUDITOR"));
            default -> false;
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank())
            return s;
        return s.substring(0, 1).toUpperCase() + (s.length() > 1 ? s.substring(1) : "");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
