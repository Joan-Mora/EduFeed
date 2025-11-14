package co.cellano.edufeed.desktop.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Biblioteca centralizada de animaciones JavaFX con duraciones y easings
 * estandarizados.
 * 
 * Principios:
 * - Micro-interacciones: 100-200ms
 * - Transiciones de panel: 200-400ms
 * - Cambios globales: 400-700ms
 * - Easing por defecto: cubic-bezier equivalente (suave pero responsivo)
 */
public class AnimationUtils {

    // Duraciones estándar
    public static final Duration MICRO = Duration.millis(120);
    public static final Duration FAST = Duration.millis(180);
    public static final Duration NORMAL = Duration.millis(220);
    public static final Duration MEDIUM = Duration.millis(300);
    public static final Duration SLOW = Duration.millis(400);

    /**
     * Fade in suave para elementos que aparecen
     */
    public static void fadeIn(Node node) {
        fadeIn(node, NORMAL);
    }

    public static void fadeIn(Node node, Duration duration) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /**
     * Fade out para elementos que desaparecen
     */
    public static void fadeOut(Node node, Runnable onFinished) {
        fadeOut(node, NORMAL, onFinished);
    }

    public static void fadeOut(Node node, Duration duration, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(node.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (onFinished != null)
                onFinished.run();
        });
        fade.play();
    }

    /**
     * Scale up con efecto de "aparición"
     */
    public static void scaleIn(Node node) {
        scaleIn(node, FAST);
    }

    public static void scaleIn(Node node, Duration duration) {
        node.setScaleX(0.85);
        node.setScaleY(0.85);
        node.setOpacity(0);

        ParallelTransition parallel = new ParallelTransition();

        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setToValue(1.0);

        parallel.getChildren().addAll(scale, fade);
        parallel.play();
    }

    /**
     * Hover effect sutil (elevación)
     */
    public static void setupHoverEffect(Node node) {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        shadow.setRadius(8);
        shadow.setOffsetY(2);

        ScaleTransition scaleUp = new ScaleTransition(MICRO, node);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);

        ScaleTransition scaleDown = new ScaleTransition(MICRO, node);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        node.setOnMouseEntered(e -> {
            node.setEffect(shadow);
            scaleUp.playFromStart();
        });

        node.setOnMouseExited(e -> {
            node.setEffect(null);
            scaleDown.playFromStart();
        });
    }

    /**
     * Slide in desde la derecha
     */
    public static void slideInRight(Node node) {
        slideInRight(node, NORMAL);
    }

    public static void slideInRight(Node node, Duration duration) {
        double originalX = node.getTranslateX();
        node.setTranslateX(300);
        node.setOpacity(0);

        ParallelTransition parallel = new ParallelTransition();

        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToX(originalX);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setToValue(1.0);

        parallel.getChildren().addAll(slide, fade);
        parallel.play();
    }

    /**
     * Slide in desde la izquierda
     */
    public static void slideInLeft(Node node) {
        slideInLeft(node, NORMAL);
    }

    public static void slideInLeft(Node node, Duration duration) {
        double originalX = node.getTranslateX();
        node.setTranslateX(-300);
        node.setOpacity(0);

        ParallelTransition parallel = new ParallelTransition();

        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setToX(originalX);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setToValue(1.0);

        parallel.getChildren().addAll(slide, fade);
        parallel.play();
    }

    /**
     * Ripple effect estilo Material Design
     */
    public static void rippleEffect(Node node, double clickX, double clickY) {
        if (!(node instanceof Pane))
            return;

        Pane pane = (Pane) node;

        Circle ripple = new Circle(0);
        ripple.setCenterX(clickX);
        ripple.setCenterY(clickY);
        ripple.setFill(Color.rgb(255, 255, 255, 0.3));

        pane.getChildren().add(ripple);

        double maxRadius = Math.max(
                Math.max(clickX, pane.getWidth() - clickX),
                Math.max(clickY, pane.getHeight() - clickY)) * 1.5;

        ParallelTransition parallel = new ParallelTransition();

        Timeline expand = new Timeline(
                new KeyFrame(Duration.millis(600),
                        new KeyValue(ripple.radiusProperty(), maxRadius)));

        FadeTransition fade = new FadeTransition(Duration.millis(600), ripple);
        fade.setToValue(0);

        parallel.getChildren().addAll(expand, fade);
        parallel.setOnFinished(e -> pane.getChildren().remove(ripple));
        parallel.play();
    }

    /**
     * Skeleton loading animation (shimmer effect)
     */
    public static Animation skeletonLoading(Rectangle skeleton) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(skeleton.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(800),
                        new KeyValue(skeleton.opacityProperty(), 0.7)),
                new KeyFrame(Duration.millis(1600),
                        new KeyValue(skeleton.opacityProperty(), 0.3)));
        timeline.setCycleCount(Animation.INDEFINITE);
        return timeline;
    }

    /**
     * Pulse animation para notificaciones
     */
    public static void pulse(Node node) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(400), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.15);
        pulse.setToY(1.15);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    /**
     * Shake animation para errores de validación
     */
    public static void shake(Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    /**
     * Contador animado (número que incrementa)
     */
    public static void animateNumber(javafx.scene.control.Label label, double from, double to, Duration duration) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(duration,
                        new KeyValue(new javafx.beans.property.SimpleDoubleProperty(from) {
                            @Override
                            protected void invalidated() {
                                label.setText(String.format("%.0f", get()));
                            }
                        }, to)));
        timeline.play();
    }
}
