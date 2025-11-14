package co.cellano.edufeed.desktop;

import co.cellano.edufeed.desktop.access.AccessCheckController;
import co.cellano.edufeed.desktop.access.LoginController;
import co.cellano.edufeed.desktop.admin.UserManagementController;
import co.cellano.edufeed.desktop.cashier.CashierController;
import co.cellano.edufeed.desktop.reports.ReportsController;
import co.cellano.edufeed.desktop.service.AccessApiClient;
import co.cellano.edufeed.desktop.session.SessionContext;
import co.cellano.edufeed.desktop.theme.ThemeService;
import co.cellano.edufeed.desktop.ui.shell.MainShell;
import co.cellano.edufeed.desktop.util.JwtUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Set;

/**
 * Aplicación Desktop EduFeed v2.0 con arquitectura moderna:
 * - MainShell (BorderPane con Sidebar + Topbar + StatusBar)
 * - ThemeService para temas intercambiables
 * - Navegación libre entre módulos sin selector
 * - Animaciones y UI responsiva
 * 
 * NOTA: Este es un ejemplo de integración. Reemplazar DesktopApp actual cuando
 * esté listo.
 */
public class DesktopAppV2 extends Application {

    private Stage stage;
    private MainShell shell;
    private String baseUrl;
    private String token;
    private Set<String> roles;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.baseUrl = System.getenv().getOrDefault("BACKEND_BASE_URL", "http://localhost:8080");
        this.token = System.getenv().getOrDefault("BACKEND_BEARER_TOKEN", "");

        if (token == null || token.isBlank()) {
            // Flujo de login
            showLogin();
        } else {
            startWithToken(token);
        }
    }

    /**
     * Muestra la pantalla de login
     */
    private void showLogin() {
        new LoginController(stage, baseUrl).start(accessToken -> {
            startWithToken(accessToken);
        });
    }

    /**
     * Inicia la aplicación con un token válido
     */
    private void startWithToken(String accessToken) {
        this.token = accessToken;
        this.roles = JwtUtils.extractRoles(token);
        String username = JwtUtils.extractUsername(token);

        // Validar que tenga al menos un rol
        if (roles == null || roles.isEmpty()) {
            new Alert(Alert.AlertType.ERROR,
                    "Tu usuario no tiene roles asignados. Contacta al administrador.",
                    ButtonType.OK).showAndWait();
            return;
        }

        // Poblar SessionContext
        SessionContext.baseUrl = baseUrl;
        SessionContext.token = token;
        SessionContext.username = username != null ? username : "Usuario";
        SessionContext.roles = roles;
        SessionContext.onLogout = this::handleLogout;

        // Crear shell principal
        initializeShell();

        // Navegar al dashboard por defecto
        navigateToModule("dashboard");
    }

    /**
     * Inicializa el shell principal de la aplicación
     */
    private void initializeShell() {
        shell = new MainShell();

        // Configurar información del usuario
        String primaryRole = getPrimaryRole();
        shell.setUserInfo(SessionContext.username, primaryRole);

        // Configurar navegación
        shell.setOnNavigate(this::navigateToModule);

        // Configurar status bar
        shell.setStatusText("● Conectado a " + baseUrl);

        // Crear escena (modo ventana; se centra abajo)
        Scene scene = new Scene(shell, 1280, 720);

        // Registrar escena en ThemeService (aplica tema automáticamente)
        ThemeService.getInstance().register(scene);

        // Configurar stage
        stage.setScene(scene);
        stage.setTitle("EduFeed — Sistema de Gestión");
        // Modo ventana centrado
        co.cellano.edufeed.desktop.util.StageUtils.centerWindow(stage, 1280, 720);

        // Atajo F11: alternar pantalla completa vs ventana centrada
        scene.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            if (evt.getCode() == KeyCode.F11) {
                evt.consume();
                boolean toFull = !stage.isFullScreen();
                stage.setFullScreen(toFull);
                if (!toFull) {
                    // Al salir de fullscreen, centra de nuevo
                    Platform.runLater(() -> co.cellano.edufeed.desktop.util.StageUtils.centerWindow(stage, 1280, 720));
                }
            }
        });

        stage.show();
    }

    /**
     * Navega a un módulo específico
     */
    private void navigateToModule(String moduleId) {
        // Actualizar sidebar
        shell.selectSidebarItem(moduleId);

        // Mantener modo ventana centrado (reajuste tras cambios de contenido grandes)
        co.cellano.edufeed.desktop.util.StageUtils.centerWindow(stage, stage.getWidth(), stage.getHeight());

        switch (moduleId) {
            case "dashboard":
                showDashboard();
                break;
            case "access":
                if (canAccess("Acceso")) {
                    showAccessModule();
                } else {
                    showAccessDenied();
                }
                break;
            case "cashier":
                if (canAccess("Caja")) {
                    showCashierModule();
                } else {
                    showAccessDenied();
                }
                break;
            case "admin":
                if (canAccess("Administración")) {
                    showAdminModule();
                } else {
                    showAccessDenied();
                }
                break;
            case "reports":
                if (canAccess("Reportes")) {
                    showReportsModule();
                } else {
                    showAccessDenied();
                }
                break;
            case "settings":
                showSettings();
                break;
            case "help":
                showHelp();
                break;
            default:
                showDashboard();
        }
    }

    /**
     * Muestra el dashboard con KPIs animados.
     */
    private void showDashboard() {
        shell.setBreadcrumbs("Dashboard");

        // Integrar módulo embebible de dashboard (si existe), sino crear uno aquí
        try {
            co.cellano.edufeed.desktop.modules.DashboardModule dm = new co.cellano.edufeed.desktop.modules.DashboardModule(
                    baseUrl, token, roles);
            shell.setContent(dm.getView());
        } catch (Throwable t) {
            // Fallback simple si el módulo no compila
            VBox fallback = new VBox();
            fallback.setSpacing(16);
            fallback.setStyle("-fx-padding: 24px;");
            Label title = new Label("Dashboard");
            title.setStyle("-fx-font-size: 32px; -fx-font-weight: 700;");
            Label error = new Label("No se pudo cargar el módulo de Dashboard: " + t.getMessage());
            error.setStyle("-fx-text-fill: #EF4444;");
            fallback.getChildren().addAll(title, error);
            shell.setContent(fallback);
        }
    }

    /**
     * Muestra el módulo de acceso
     */
    private void showAccessModule() {
        shell.setBreadcrumbs("Acceso", "Control de Entrada");

        // Usar el nuevo AccessCheckModuleV2 con QR y autenticación biométrica
        co.cellano.edufeed.desktop.modules.AccessCheckModuleV2 accessModule = new co.cellano.edufeed.desktop.modules.AccessCheckModuleV2(
                baseUrl, token);

        shell.setContent(accessModule.getView());
    }

    /**
     * Muestra el módulo de caja
     */
    private void showCashierModule() {
        shell.setBreadcrumbs("Caja", "Punto de Venta");

        // Crear e integrar CashierModule
        co.cellano.edufeed.desktop.cashier.CashierModule cashierModule = new co.cellano.edufeed.desktop.cashier.CashierModule(
                baseUrl, token);

        shell.setContent(cashierModule.getView());
    }

    /**
     * Muestra el módulo de administración
     */
    private void showAdminModule() {
        shell.setBreadcrumbs("Administración", "Usuarios");

        // Crear e integrar UserManagementModule
        co.cellano.edufeed.desktop.modules.UserManagementModule userModule = new co.cellano.edufeed.desktop.modules.UserManagementModule(
                baseUrl, token);

        shell.setContent(userModule.getView());
    }

    /**
     * Muestra el módulo de reportes
     */
    private void showReportsModule() {
        shell.setBreadcrumbs("Reportes", "Análisis");

        // Crear e integrar ReportsModule
        co.cellano.edufeed.desktop.reports.ReportsModule reportsModule = new co.cellano.edufeed.desktop.reports.ReportsModule(
                baseUrl, token);

        shell.setContent(reportsModule.getView());
    }

    /**
     * Muestra configuración de la aplicación
     */
    private void showSettings() {
        shell.setBreadcrumbs("Configuración");

        VBox settingsBox = new VBox();
        settingsBox.setSpacing(20);
        settingsBox.setStyle("-fx-padding: 20px;");

        Label title = new Label("Configuración");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 600;");

        Label themeLabel = new Label("Cambiar tema desde el selector en la barra superior (🎨)");

        settingsBox.getChildren().addAll(title, themeLabel);
        shell.setContent(settingsBox);
    }

    /**
     * Muestra ayuda
     */
    private void showHelp() {
        shell.setBreadcrumbs("Ayuda");

        VBox helpBox = new VBox();
        helpBox.setSpacing(20);
        helpBox.setStyle("-fx-padding: 20px;");

        Label title = new Label("Ayuda");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 600;");

        Label info = new Label("Sistema EduFeed v2.0\nNavegación moderna con sidebar colapsable.");

        helpBox.getChildren().addAll(title, info);
        shell.setContent(helpBox);
    }

    /**
     * Muestra mensaje de acceso denegado
     */
    private void showAccessDenied() {
        shell.clearContent();

        VBox deniedBox = new VBox();
        deniedBox.setSpacing(10);
        deniedBox.setStyle("-fx-padding: 40px; -fx-alignment: center;");

        Label title = new Label("⛔ Acceso Denegado");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: #EF4444;");

        Label message = new Label("No tienes permisos para acceder a este módulo.");
        message.setStyle("-fx-font-size: 16px; -fx-text-fill: #6B7280;");

        deniedBox.getChildren().addAll(title, message);
        shell.addContent(deniedBox);
    }

    /**
     * Verifica si el usuario puede acceder a un módulo
     */
    private boolean canAccess(String module) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }

        return switch (module) {
            case "Caja" -> roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OPERADOR_CAJA");
            case "Acceso" -> roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OPERADOR_ACCESO");
            case "Administración" -> roles.contains("ROLE_ADMIN");
            case "Reportes" -> roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPERVISOR");
            default -> false;
        };
    }

    /**
     * Obtiene el rol principal del usuario
     */
    private String getPrimaryRole() {
        if (roles.contains("ROLE_ADMIN")) {
            return "Administrador";
        } else if (roles.contains("ROLE_SUPERVISOR")) {
            return "Supervisor";
        } else if (roles.contains("ROLE_OPERADOR_CAJA")) {
            return "Operador de Caja";
        } else if (roles.contains("ROLE_OPERADOR_ACCESO")) {
            return "Operador de Acceso";
        }
        return "Usuario";
    }

    /**
     * Maneja el cierre de sesión
     */
    private void handleLogout() {
        // Limpiar contexto
        SessionContext.baseUrl = null;
        SessionContext.token = null;
        SessionContext.username = null;
        SessionContext.roles = null;

        // Volver al login
        showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
