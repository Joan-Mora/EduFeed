package co.cellano.edufeed.desktop.ui;

import co.cellano.edufeed.desktop.theme.ThemeService;
import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.scene.Node;
import javafx.scene.Scene;

/**
 * Utilidades de UI: aplicar tema y animaciones.
 * DEPRECADO: Usar ThemeService y AnimationUtils directamente.
 * Mantenido por compatibilidad con código existente.
 */
@Deprecated
public final class UIUtils {
    private UIUtils() {
    }

    /**
     * Aplica el tema usando el nuevo ThemeService
     * 
     * @deprecated Usar ThemeService.getInstance().register(scene)
     */
    @Deprecated
    public static void applyTheme(Scene scene) {
        if (scene == null)
            return;
        ThemeService.getInstance().register(scene);
    }

    /**
     * Fade in simple
     * 
     * @deprecated Usar AnimationUtils.fadeIn(node)
     */
    @Deprecated
    public static void fadeIn(Node node) {
        if (node == null)
            return;
        AnimationUtils.fadeIn(node);
    }
}
