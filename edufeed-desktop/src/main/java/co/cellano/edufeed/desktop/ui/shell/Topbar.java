package co.cellano.edufeed.desktop.ui.shell;

import co.cellano.edufeed.desktop.theme.ThemeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Consumer;

/**
 * Barra superior de la aplicación con:
 * - Breadcrumb de navegación
 * - Campo de búsqueda global
 * - Botones de acción rápida
 * - Información de usuario
 * - Selector de tema
 */
public class Topbar extends HBox {

    private final HBox breadcrumbContainer;
    private final TextField searchField;
    private final Label userLabel;
    private final MenuButton themeSelector;

    private Consumer<ThemeService.Theme> onThemeChange;

    public Topbar() {
        getStyleClass().add("app-topbar");
        setSpacing(20);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(12, 24, 12, 24));

        // Texto estático con indicación del atajo Ctrl+K
        breadcrumbContainer = new HBox();
        breadcrumbContainer.getStyleClass().add("app-topbar__breadcrumb");
        breadcrumbContainer.setSpacing(8);
        breadcrumbContainer.setAlignment(Pos.CENTER_LEFT);

        // Label estático para accesos directos
        Label shortcutLabel = new Label("⚡ Accesos Directos - CTRL + K");
        shortcutLabel.getStyleClass().add("app-topbar__breadcrumb-item");
        shortcutLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px; " +
                "-fx-text-fill: rgba(255, 255, 255, 0.9); " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 2, 0.5, 0, 1);");
        breadcrumbContainer.getChildren().add(shortcutLabel);

        // Campo de búsqueda eliminado - usar solo Ctrl+K
        searchField = null;

        // Spacer para empujar acciones a la derecha
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Acciones rápidas
        HBox actions = new HBox();
        actions.getStyleClass().add("app-topbar__actions");
        actions.setSpacing(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Botón notificaciones
        Button notifBtn = new Button("🔔");
        notifBtn.getStyleClass().add("app-topbar__button");
        notifBtn.setTooltip(new Tooltip("Notificaciones"));

        // Botón ayuda
        Button helpBtn = new Button("?");
        helpBtn.getStyleClass().add("app-topbar__button");
        helpBtn.setTooltip(new Tooltip("Ayuda"));

        // Selector de tema
        themeSelector = new MenuButton("✨");
        themeSelector.getStyleClass().add("app-topbar__button");
        themeSelector.setTooltip(new Tooltip("Cambiar tema"));

        for (ThemeService.Theme theme : ThemeService.Theme.values()) {
            MenuItem item = new MenuItem(theme.getDisplayName());
            item.setOnAction(e -> {
                if (onThemeChange != null) {
                    onThemeChange.accept(theme);
                }
            });
            themeSelector.getItems().add(item);
        }

        // Usuario
        HBox userBox = new HBox();
        userBox.getStyleClass().add("app-topbar__user");
        userBox.setSpacing(10);
        userBox.setAlignment(Pos.CENTER);

        Region avatar = new Region();
        avatar.getStyleClass().add("app-topbar__user-avatar");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);

        userLabel = new Label("Usuario");
        userLabel.getStyleClass().add("app-topbar__user-name");
        // Asegurar texto blanco visible
        userLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600; -fx-font-size: 14px;");

        userBox.getChildren().addAll(avatar, userLabel);

        actions.getChildren().addAll(notifBtn, helpBtn, themeSelector, userBox);

        getChildren().addAll(breadcrumbContainer, spacer, actions);
    }

    /**
     * Actualiza el breadcrumb (deshabilitado - ahora usa texto estático)
     */
    public void updateBreadcrumbs(List<String> items) {
        // El breadcrumb ahora es estático: "⚡ Accesos Directos - CTRL + K"
        // Este método se mantiene por compatibilidad pero no hace nada
    }

    /**
     * Establece información del usuario
     */
    public void setUserInfo(String username, String role) {
        if (role != null && !role.isEmpty()) {
            userLabel.setText(username + " (" + role + ")");
        } else {
            userLabel.setText(username);
        }
    }

    /**
     * Callback cuando cambia el tema
     */
    public void setOnThemeChange(Consumer<ThemeService.Theme> callback) {
        this.onThemeChange = callback;
    }

    /**
     * Obtiene el campo de búsqueda
     */
    public TextField getSearchField() {
        return searchField;
    }
}
