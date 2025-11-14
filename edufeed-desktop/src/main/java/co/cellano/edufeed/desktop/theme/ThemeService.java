package co.cellano.edufeed.desktop.theme;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Servicio centralizado de gestión de temas con soporte para:
 * - Múltiples paletas (Light, Dark, Corporativo, Vibrant)
 * - Transiciones animadas entre temas
 * - Persistencia de preferencias
 * - Variables CSS dinámicas
 */
public class ThemeService {

    private static final ThemeService INSTANCE = new ThemeService();
    private static final String PREF_THEME = "app.theme";

    private Theme currentTheme;
    private final List<Scene> registeredScenes = new ArrayList<>();
    private Preferences prefs;

    private ThemeService() {
        // Inicializar preferencias de forma segura
        try {
            prefs = Preferences.userNodeForPackage(ThemeService.class);
            String savedTheme = prefs.get(PREF_THEME, Theme.LIGHT.name());
            currentTheme = Theme.valueOf(savedTheme);
        } catch (Exception e) {
            // Si falla, usar tema por defecto
            currentTheme = Theme.LIGHT;
            prefs = null;
        }
    }

    public static ThemeService getInstance() {
        return INSTANCE;
    }

    /**
     * Registra una escena para aplicar temas automáticamente
     */
    public void register(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
            applyThemeToScene(scene, currentTheme, false);
        }
    }

    /**
     * Desregistra una escena
     */
    public void unregister(Scene scene) {
        registeredScenes.remove(scene);
    }

    /**
     * Cambia el tema global con transición animada
     */
    public void setTheme(Theme theme, boolean animated) {
        if (this.currentTheme == theme) {
            return;
        }

        this.currentTheme = theme;

        // Guardar preferencia de forma segura
        if (prefs != null) {
            try {
                prefs.put(PREF_THEME, theme.name());
            } catch (Exception e) {
                // Ignorar si no se puede guardar
            }
        }

        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene, theme, animated);
        }
    }

    public void setTheme(Theme theme) {
        setTheme(theme, true);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Aplica un tema a una escena específica
     */
    private void applyThemeToScene(Scene scene, Theme theme, boolean animated) {
        scene.getStylesheets().clear();

        // Cargar hojas de estilo base
        scene.getStylesheets().add(getClass().getResource("/styles/layout.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
        // Hoja de estilos de la app (navbar, overlays, etc.)
        var appCss = getClass().getResource("/styles/app.css");
        if (appCss != null) {
            scene.getStylesheets().add(appCss.toExternalForm());
        }

        // Cargar tema específico
        scene.getStylesheets().add(getClass().getResource(theme.getStylesheetPath()).toExternalForm());

        if (animated) {
            // Animación de transición suave (fade)
            scene.getRoot().setOpacity(0.85);
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(400),
                            new KeyValue(scene.getRoot().opacityProperty(), 1.0)));
            timeline.play();
        }
    }

    /**
     * Obtiene las variables CSS del tema actual
     */
    public Map<String, String> getCurrentThemeVariables() {
        return currentTheme.getVariables();
    }

    /**
     * Enumeración de temas disponibles
     */
    public enum Theme {
        LIGHT("Light", "/styles/theme-light.css", createLightVariables()),
        VIBRANT("Vibrant", "/styles/theme-vibrant.css", createVibrantVariables()),
        DARK("Dark (en desarrollo)", "/styles/theme-dark.css", createDarkVariables());

        private final String displayName;
        private final String stylesheetPath;
        private final Map<String, String> variables;

        Theme(String displayName, String stylesheetPath, Map<String, String> variables) {
            this.displayName = displayName;
            this.stylesheetPath = stylesheetPath;
            this.variables = variables;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStylesheetPath() {
            return stylesheetPath;
        }

        public Map<String, String> getVariables() {
            return variables;
        }

        private static Map<String, String> createLightVariables() {
            Map<String, String> vars = new HashMap<>();
            vars.put("primary", "#0B78D1");
            vars.put("secondary", "#6B7280");
            vars.put("accent", "#FFD400");
            vars.put("background", "#F7F8FA");
            vars.put("surface", "#FFFFFF");
            vars.put("text-primary", "#0A0A0A");
            vars.put("text-secondary", "#6B7280");
            vars.put("border", "#E6E9EE");
            vars.put("success", "#10B981");
            vars.put("warning", "#F59E0B");
            vars.put("error", "#EF4444");
            return vars;
        }

        private static Map<String, String> createDarkVariables() {
            Map<String, String> vars = new HashMap<>();
            vars.put("primary", "#3B82F6");
            vars.put("secondary", "#9CA3AF");
            vars.put("accent", "#FFD400");
            vars.put("background", "#0F172A");
            vars.put("surface", "#1E293B");
            vars.put("text-primary", "#F1F5F9");
            vars.put("text-secondary", "#94A3B8");
            vars.put("border", "#334155");
            vars.put("success", "#10B981");
            vars.put("warning", "#F59E0B");
            vars.put("error", "#EF4444");
            return vars;
        }

        private static Map<String, String> createVibrantVariables() {
            Map<String, String> vars = new HashMap<>();
            vars.put("primary", "#8B5CF6");
            vars.put("secondary", "#EC4899");
            vars.put("accent", "#F59E0B");
            vars.put("background", "#FAF5FF");
            vars.put("surface", "#FFFFFF");
            vars.put("text-primary", "#1F2937");
            vars.put("text-secondary", "#6B7280");
            vars.put("border", "#E9D5FF");
            vars.put("success", "#10B981");
            vars.put("warning", "#F59E0B");
            vars.put("error", "#EF4444");
            return vars;
        }
    }
}
