package co.cellano.edufeed.desktop.ui.shell;

import co.cellano.edufeed.desktop.theme.ThemeService;
import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shell principal de la aplicación con arquitectura BorderPane:
 * - Left: Sidebar navegable y colapsable
 * - Top: Topbar con breadcrumb, búsqueda y usuario
 * - Center: Área de contenido dinámico
 * - Bottom: StatusBar con información del sistema
 */
public class MainShell extends BorderPane {

    private final Sidebar sidebar;
    private final Topbar topbar;
    private final VBox contentArea;
    private final StatusBar statusBar;

    private final List<String> breadcrumbs = new ArrayList<>();
    private Consumer<String> onNavigate;

    public MainShell() {
        getStyleClass().add("app-shell");

        // Crear componentes
        sidebar = new Sidebar();
        topbar = new Topbar();
        contentArea = new VBox();
        statusBar = new StatusBar();

        // Configurar content area
        contentArea.getStyleClass().add("app-content");
        contentArea.setSpacing(24);
        contentArea.setFillWidth(true);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Scroll para el content area dentro de un contenedor apilado que permitirá
        // overlays (búsqueda global)
        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("scroll-pane");
        centerContainer = new StackPane(scrollPane);
        centerContainer.getStyleClass().add("app-center-wrapper");

        // Ensamblar shell
        setLeft(sidebar);
        setTop(topbar);
        setCenter(centerContainer);
        setBottom(statusBar);

        // Configurar callbacks
        sidebar.setOnNavigate(this::handleNavigation);
        topbar.setOnThemeChange(theme -> ThemeService.getInstance().setTheme(theme));

        // Breadcrumb inicial
        setBreadcrumbs("Dashboard");

        // Registrar atajo global Ctrl+K para búsqueda rápida
        sceneHookForGlobalSearch();
    }

    /**
     * Establece el contenido principal con transición suave
     */
    public void setContent(Node content) {
        if (!contentArea.getChildren().isEmpty()) {
            // Fade out contenido anterior
            Node oldContent = contentArea.getChildren().get(0);
            AnimationUtils.fadeOut(oldContent, AnimationUtils.FAST, () -> {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(content);
                AnimationUtils.fadeIn(content, AnimationUtils.FAST);
                AnimationUtils.slideInLeft(content, AnimationUtils.FAST);
            });
        } else {
            contentArea.getChildren().add(content);
            AnimationUtils.fadeIn(content, AnimationUtils.FAST);
            AnimationUtils.slideInLeft(content, AnimationUtils.FAST);
        }
    }

    /**
     * Añade contenido al área principal
     */
    public void addContent(Node content) {
        contentArea.getChildren().add(content);
        AnimationUtils.fadeIn(content);
    }

    /**
     * Limpia el área de contenido
     */
    public void clearContent() {
        contentArea.getChildren().clear();
    }

    /**
     * Establece el breadcrumb
     */
    public void setBreadcrumbs(String... items) {
        breadcrumbs.clear();
        for (String item : items) {
            breadcrumbs.add(item);
        }
        topbar.updateBreadcrumbs(breadcrumbs);
    }

    /**
     * Selecciona un item del sidebar programáticamente
     */
    public void selectSidebarItem(String itemId) {
        sidebar.selectItem(itemId);
    }

    /**
     * Callback cuando se navega a un módulo
     */
    public void setOnNavigate(Consumer<String> callback) {
        this.onNavigate = callback;
    }

    /**
     * Actualiza información del usuario en topbar
     */
    public void setUserInfo(String username, String role) {
        topbar.setUserInfo(username, role);
    }

    /**
     * Actualiza el status bar
     */
    public void setStatusText(String text) {
        statusBar.setText(text);
    }

    /**
     * Colapsa o expande el sidebar
     */
    public void toggleSidebar() {
        sidebar.toggle();
    }

    /**
     * Obtiene la referencia al sidebar
     */
    public Sidebar getSidebar() {
        return sidebar;
    }

    /**
     * Obtiene la referencia al topbar
     */
    public Topbar getTopbar() {
        return topbar;
    }

    private void handleNavigation(String moduleId) {
        if (onNavigate != null) {
            onNavigate.accept(moduleId);
        }
    }

    // --- Global Search Overlay (Ctrl+K) ---
    private GlobalSearchOverlay searchOverlay;
    private StackPane centerContainer; // wrapper para contenido + overlays

    private void sceneHookForGlobalSearch() {
        // El Scene se establece después en DesktopAppV2; usar listener
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, evt -> {
                    if ((evt.isControlDown() || evt.isMetaDown()) && evt.getCode() == javafx.scene.input.KeyCode.K) {
                        evt.consume();
                        toggleGlobalSearch();
                    } else if (evt.getCode() == javafx.scene.input.KeyCode.ESCAPE && searchOverlay != null
                            && searchOverlay.isVisible()) {
                        evt.consume();
                        hideGlobalSearch();
                    }
                });
            }
        });
    }

    private void toggleGlobalSearch() {
        if (isGlobalSearchOpen()) {
            hideGlobalSearch();
        } else {
            showGlobalSearch();
        }
    }

    private boolean isGlobalSearchOpen() {
        if (centerContainer == null)
            return false;
        return centerContainer.getChildren().stream()
                .anyMatch(n -> n.getStyleClass().contains("global-search__glass"));
    }

    private void showGlobalSearch() {
        if (searchOverlay == null) {
            searchOverlay = new GlobalSearchOverlay(query -> {
                // navegar al módulo seleccionado
                hideGlobalSearch();
                selectSidebarItem(query);
                if (onNavigate != null)
                    onNavigate.accept(query);
            });
        }
        if (centerContainer == null)
            return;
        // Asegurar que el overlay vuelve a estar visible (tras haber sido ocultado
        // anteriormente)
        searchOverlay.setVisible(true);

        // Crear capa de vidrio si no existe
        var existingGlass = centerContainer.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("global-search__glass"))
                .findFirst()
                .orElse(null);

        if (existingGlass == null) {
            StackPane glass = new StackPane(searchOverlay);
            glass.getStyleClass().add("global-search__glass");
            glass.setPickOnBounds(true);
            searchOverlay.setOnCancel(this::hideGlobalSearch);
            centerContainer.getChildren().add(glass);
            AnimationUtils.fadeIn(glass, AnimationUtils.FAST);
        } else {
            existingGlass.setVisible(true);
            AnimationUtils.fadeIn(existingGlass, AnimationUtils.FAST);
        }

        // Limpiar query y dar foco DESPUÉS de mostrar
        searchOverlay.clearQuery();
        searchOverlay.requestFocusQuery();
    }

    private void hideGlobalSearch() {
        if (centerContainer != null) {
            var toRemove = centerContainer.getChildren().stream()
                    .filter(n -> n.getStyleClass().contains("global-search__glass"))
                    .findFirst();
            toRemove.ifPresent(node -> AnimationUtils.fadeOut(node, AnimationUtils.FAST, () -> {
                centerContainer.getChildren().remove(node);
                if (searchOverlay != null) {
                    searchOverlay.setVisible(false);
                }
            }));
        }
    }

    /**
     * Clase interna: Sidebar colapsable
     */
    public static class Sidebar extends VBox {
        private boolean collapsed = false;
        private final VBox nav;
        private final List<SidebarItem> items = new ArrayList<>();
        private Consumer<String> onNavigate;
        private final Label logo;
        private final Label appName;

        public Sidebar() {
            getStyleClass().add("app-sidebar");
            setMinWidth(240);
            setMaxWidth(240);

            // Header con logo más grande y prominente
            VBox header = new VBox(16);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(32, 16, 32, 16));
            header.getStyleClass().add("app-sidebar__header");

            // Logo con tamaño máximo y transformación de escala
            logo = new Label("🎓");
            logo.getStyleClass().add("app-sidebar__logo");
            logo.setFont(javafx.scene.text.Font.font(96)); // Aumentado a 96px
            logo.setScaleX(1.5); // Escala adicional 150%
            logo.setScaleY(1.5); // Escala adicional 150%
            logo.setStyle("-fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.5), 12, 0.7, 0, 4);");

            // Nombre de la app muy grande
            appName = new Label("EduFeed");
            appName.getStyleClass().add("app-sidebar__app-name");
            appName.setFont(javafx.scene.text.Font.font(42)); // Aumentado a 42px
            appName.setStyle("-fx-font-weight: 900; -fx-text-fill: white; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 10, 0.8, 0, 4);");

            header.getChildren().addAll(logo, appName);

            // Botón hamburguesa mejorado y llamativo
            javafx.scene.control.Button hamburgerBtn = new javafx.scene.control.Button("☰");
            hamburgerBtn.getStyleClass().addAll("app-button", "app-button--ghost", "hamburger-menu");
            hamburgerBtn.setStyle(
                    "-fx-font-size: 24px; " +
                            "-fx-min-width: 48px; " +
                            "-fx-min-height: 48px; " +
                            "-fx-max-width: 48px; " +
                            "-fx-max-height: 48px; " +
                            "-fx-padding: 0; " +
                            "-fx-background-color: rgba(255, 255, 255, 0.1); " +
                            "-fx-text-fill: white; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 12; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 2);");

            // Efectos hover mejorados
            hamburgerBtn.setOnMouseEntered(e -> {
                hamburgerBtn.setStyle(
                        "-fx-font-size: 24px; " +
                                "-fx-min-width: 48px; " +
                                "-fx-min-height: 48px; " +
                                "-fx-max-width: 48px; " +
                                "-fx-max-height: 48px; " +
                                "-fx-padding: 0; " +
                                "-fx-background-color: rgba(255, 255, 255, 0.25); " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 12; " +
                                "-fx-border-color: rgba(255, 255, 255, 0.4); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.4), 12, 0, 0, 4); " +
                                "-fx-scale-x: 1.05; " +
                                "-fx-scale-y: 1.05;");
            });

            hamburgerBtn.setOnMouseExited(e -> {
                hamburgerBtn.setStyle(
                        "-fx-font-size: 24px; " +
                                "-fx-min-width: 48px; " +
                                "-fx-min-height: 48px; " +
                                "-fx-max-width: 48px; " +
                                "-fx-max-height: 48px; " +
                                "-fx-padding: 0; " +
                                "-fx-background-color: rgba(255, 255, 255, 0.1); " +
                                "-fx-text-fill: white; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 12; " +
                                "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 2);");
            });

            hamburgerBtn.setOnAction(e -> {
                // Animación de rotación más rápida y sincronizada con el toggle (00ms)
                RotateTransition rotate = new RotateTransition(Duration.millis(400), hamburgerBtn);
                rotate.setByAngle(180);
                rotate.setInterpolator(Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0)); // Mismo interpolador que el ancho
                rotate.play();

                // Toggle del sidebar
                toggle();
            });

            // Nav container
            nav = new VBox(4);
            nav.setPadding(new Insets(0, 8, 0, 8));

            addItem("dashboard", "🏠", "Dashboard");
            addItem("access", "🚪", "Acceso");
            addItem("cashier", "💰", "Caja");
            addItem("admin", "👥", "Admin Usuarios");
            addItem("reports", "📊", "Reportes");
            addItem("settings", "⚙", "Configuración");
            addItem("help", "❓", "Ayuda");

            // Spacer para empujar el botón hamburguesa al fondo
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);

            // Contenedor para centrar el botón hamburguesa
            HBox hamburgerContainer = new HBox(hamburgerBtn);
            hamburgerContainer.setAlignment(Pos.CENTER);
            hamburgerContainer.setPadding(new Insets(12, 0, 16, 0));

            getChildren().addAll(header, nav, spacer, hamburgerContainer);
        }

        private void addItem(String id, String icon, String text) {
            SidebarItem item = new SidebarItem(id, icon, text);
            item.setOnAction(() -> {
                selectItem(id);
                if (onNavigate != null) {
                    onNavigate.accept(id);
                }
            });
            items.add(item);
            nav.getChildren().add(item);
        }

        public void selectItem(String id) {
            for (SidebarItem item : items) {
                item.setActive(item.getItemId().equals(id));
            }
        }

        public void setOnNavigate(Consumer<String> callback) {
            this.onNavigate = callback;
        }

        public void toggle() {
            collapsed = !collapsed;

            // Duración más corta y rápida para mejor fluidez (300ms en lugar de 400ms)
            Duration duration = Duration.millis(400);

            // Valores de inicio y fin
            double fromWidth = collapsed ? 240 : 64;
            double toWidth = collapsed ? 64 : 240;

            // Actualizar clases CSS al inicio
            if (collapsed) {
                getStyleClass().add("app-sidebar--collapsed");
            } else {
                getStyleClass().remove("app-sidebar--collapsed");
            }

            // Usar interpolador más rápido y natural (ease-out para sensación más fluida)
            Interpolator smoothInterpolator = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);

            // Animación de ancho sincronizada en una sola Timeline
            Timeline widthAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(minWidthProperty(), fromWidth, smoothInterpolator),
                            new KeyValue(maxWidthProperty(), fromWidth, smoothInterpolator),
                            new KeyValue(prefWidthProperty(), fromWidth, smoothInterpolator)),
                    new KeyFrame(duration,
                            new KeyValue(minWidthProperty(), toWidth, smoothInterpolator),
                            new KeyValue(maxWidthProperty(), toWidth, smoothInterpolator),
                            new KeyValue(prefWidthProperty(), toWidth, smoothInterpolator)));

            // Animación solo del texto "EduFeed", el logo (sombrerito) se mantiene visible
            if (collapsed) {
                // Fade out instantáneo del texto
                FadeTransition textFade = new FadeTransition(Duration.millis(100), appName);
                textFade.setFromValue(1.0);
                textFade.setToValue(0.0);
                textFade.setInterpolator(Interpolator.LINEAR);
                textFade.setOnFinished(e -> appName.setVisible(false));
                textFade.play();
                // El logo permanece visible
            } else {
                // Fade in del texto con delay
                appName.setVisible(true);
                appName.setOpacity(0.0);
                PauseTransition pause = new PauseTransition(Duration.millis(150));
                pause.setOnFinished(e -> {
                    FadeTransition textFade = new FadeTransition(Duration.millis(150), appName);
                    textFade.setFromValue(0.0);
                    textFade.setToValue(1.0);
                    textFade.setInterpolator(Interpolator.LINEAR);
                    textFade.play();
                });
                pause.play();
                // El logo siempre está visible
            }

            // Animación de opacidad para los textos - más rápida y sincronizada
            for (SidebarItem item : items) {
                if (collapsed) {
                    // Colapsar: fade out instantáneo
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(400), item.textLabel);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setInterpolator(Interpolator.LINEAR);
                    fadeOut.setOnFinished(e -> {
                        item.textLabel.setVisible(false);
                        item.textLabel.setScaleX(1.0);
                        item.textLabel.setScaleY(1.0);
                    });
                    fadeOut.play();

                } else {
                    // Expandir: fade in rápido con menos delay
                    item.textLabel.setVisible(true);
                    item.textLabel.setOpacity(0.0);
                    item.textLabel.setScaleX(1.0);
                    item.textLabel.setScaleY(1.0);

                    PauseTransition pause = new PauseTransition(Duration.millis(400));
                    pause.setOnFinished(e -> {
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), item.textLabel);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.setInterpolator(Interpolator.LINEAR);
                        fadeIn.play();
                    });
                    pause.play();
                }
            }

            // Iniciar animación de ancho
            widthAnimation.play();
        }
    }

    /**
     * Clase interna: Item individual del sidebar
     */
    private static class SidebarItem extends HBox {
        private final String id;
        private final Label iconLabel;
        private final Label textLabel;
        private Runnable onAction;

        public SidebarItem(String id, String icon, String text) {
            this.id = id;
            getStyleClass().add("app-sidebar__item");
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(12);
            setPadding(new Insets(12, 16, 12, 16));

            iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("app-sidebar__item-icon");
            iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;"); // Tamaño original con color blanco
            iconLabel.setMinWidth(28);
            iconLabel.setAlignment(Pos.CENTER);

            textLabel = new Label(text);
            textLabel.getStyleClass().add("app-sidebar__item-text");

            getChildren().addAll(iconLabel, textLabel);

            setOnMouseClicked(e -> {
                if (onAction != null) {
                    onAction.run();
                }
            });

            AnimationUtils.setupHoverEffect(this);
        }

        public String getItemId() {
            return id;
        }

        public void setActive(boolean active) {
            if (active) {
                getStyleClass().add("app-sidebar__item--active");
            } else {
                getStyleClass().remove("app-sidebar__item--active");
            }
        }

        public void setOnAction(Runnable action) {
            this.onAction = action;
        }
    }

    // --- Overlay de Búsqueda Global ---
    private static class GlobalSearchOverlay extends VBox {
        private final javafx.scene.control.TextField queryField = new javafx.scene.control.TextField();
        private final javafx.scene.control.ListView<String> results = new javafx.scene.control.ListView<>();
        private final java.util.Map<String, String> labelById = new java.util.LinkedHashMap<>();
        private java.util.function.Consumer<String> onSelect;
        private Runnable onCancel;

        GlobalSearchOverlay(java.util.function.Consumer<String> onSelect) {
            this.onSelect = onSelect;
            getStyleClass().add("global-search");
            setSpacing(8);
            setPadding(new Insets(16));
            setMaxWidth(400);
            setStyle(
                    "-fx-background-color: -fx-surface; -fx-background-radius: 12; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.25), 12, 0, 0, 4);");
            // Registrar destinos
            labelById.put("dashboard", "Dashboard");
            labelById.put("access", "Punto de Acceso");
            labelById.put("cashier", "Caja / Pagos");
            labelById.put("admin", "Administración de Usuarios");
            labelById.put("reports", "Reportes y Análisis");
            labelById.put("settings", "Configuración");
            labelById.put("help", "Ayuda");
            queryField.setPromptText("Buscar módulo...");
            results.setFocusTraversable(false);
            results.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(labelById.getOrDefault(item, item));
                    }
                }
            });
            getChildren().addAll(queryField, results);
            queryField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));
            results.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    String id = results.getSelectionModel().getSelectedItem();
                    if (id != null && onSelect != null)
                        onSelect.accept(id);
                }
            });
            queryField.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER -> {
                        String id = results.getSelectionModel().getSelectedItem();
                        if (id != null && onSelect != null)
                            onSelect.accept(id);
                    }
                    case ESCAPE -> {
                        if (onCancel != null)
                            onCancel.run();
                    }
                    case DOWN -> results.requestFocus();
                    default -> {
                    }
                }
            });
            results.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER -> {
                        String id = results.getSelectionModel().getSelectedItem();
                        if (id != null && onSelect != null)
                            onSelect.accept(id);
                    }
                    case ESCAPE -> {
                        if (onCancel != null)
                            onCancel.run();
                    }
                    default -> {
                    }
                }
            });
            applyFilter("");
        }

        void applyFilter(String q) {
            String low = q == null ? "" : q.toLowerCase();
            results.getItems().setAll(labelById.entrySet().stream()
                    .filter(e -> e.getValue().toLowerCase().contains(low))
                    .map(java.util.Map.Entry::getKey)
                    .toList());
            if (!results.getItems().isEmpty())
                results.getSelectionModel().select(0);
        }

        void clearQuery() {
            queryField.clear();
            applyFilter("");
        }

        void requestFocusQuery() {
            javafx.application.Platform.runLater(queryField::requestFocus);
        }

        void setOnCancel(Runnable r) {
            this.onCancel = r;
        }
    }
}
