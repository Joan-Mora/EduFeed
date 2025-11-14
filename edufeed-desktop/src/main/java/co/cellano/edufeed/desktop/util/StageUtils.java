package co.cellano.edufeed.desktop.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Utilidades relacionadas con el {@link Stage}.
 * Centraliza la política de que la aplicación debe ejecutarse siempre en
 * pantalla completa
 * (maximizada) salvo que se decida explícitamente lo contrario en el futuro.
 */
public final class StageUtils {

    private StageUtils() {
    }

    /**
     * Garantiza (lo mejor posible) que el Stage quede maximizado. Algunos entornos
     * necesitan forzar nuevamente tras el siguiente pulso de UI o cuando el tamaño
     * actual no coincide con el área visual disponible (multi-monitor / escala).
     */
    public static void ensureMaximized(Stage stage) {
        if (stage == null)
            return;
        try {
            stage.setMaximized(true);
            // Reforzar en el siguiente pulso de UI y validar contra el tamaño de pantalla
            Platform.runLater(() -> {
                Rectangle2D vb = visualBoundsFor(stage);
                boolean looksWindowed = (stage.getWidth() < vb.getWidth() * 0.9)
                        || (stage.getHeight() < vb.getHeight() * 0.9);
                if (!stage.isMaximized() || looksWindowed) {
                    stage.setMaximized(true);
                }
            });
        } catch (Exception ignored) {
            // No bloquear flujo por un fallo aquí
        }
    }

    /**
     * Centra el Stage en la pantalla activa y ajusta el tamaño solicitado,
     * respetando los
     * límites del área visual (sin solapar barras del sistema).
     */
    public static void centerWindow(Stage stage, double width, double height) {
        if (stage == null)
            return;
        try {
            Rectangle2D vb = visualBoundsFor(stage);
            double w = Math.min(width, vb.getWidth());
            double h = Math.min(height, vb.getHeight());
            stage.setWidth(w);
            stage.setHeight(h);
            stage.setX(vb.getMinX() + (vb.getWidth() - w) / 2);
            stage.setY(vb.getMinY() + (vb.getHeight() - h) / 2);
            // Reforzar centrado después del primer pulso de UI
            Platform.runLater(() -> stage.centerOnScreen());
        } catch (Exception ignored) {
        }
    }

    private static Rectangle2D visualBoundsFor(Stage stage) {
        try {
            Rectangle2D rect = new Rectangle2D(stage.getX(), stage.getY(), Math.max(stage.getWidth(), 1),
                    Math.max(stage.getHeight(), 1));
            var screens = Screen.getScreensForRectangle(rect);
            if (screens != null && !screens.isEmpty()) {
                return screens.get(0).getVisualBounds();
            }
        } catch (Exception ignored) {
        }
        return Screen.getPrimary().getVisualBounds();
    }
}
