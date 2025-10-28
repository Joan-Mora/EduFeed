package co.cellano.edufeed.desktop.access;

import java.awt.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class AccessCheckView extends BorderPane {
    private final Label title = new Label();
    private final Label detail = new Label();
    private final Circle icon = new Circle(60);

    public AccessCheckView() {
        setPadding(new Insets(16));
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        detail.setStyle("-fx-font-size: 14;");

        var center = new StackPane(icon);
        center.setPadding(new Insets(12));
        setTop(title); BorderPane.setAlignment(title, Pos.CENTER);
        setCenter(center);
        setBottom(detail); BorderPane.setAlignment(detail, Pos.CENTER);
    }

    public void showApproved(String message) {
        title.setText("APROBADO ✅");
        detail.setText(message);
        icon.setFill(Color.web("#28a745"));
        animate();
        beep();
    }

    public void showDenied(String message) {
        title.setText("DENEGADO ❌");
        detail.setText(message);
        icon.setFill(Color.web("#dc3545"));
        animate();
        beep();
    }

    private void animate() {
        FadeTransition ft = new FadeTransition(Duration.millis(600), icon);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.play();
    }

    private void beep() {
        try { Toolkit.getDefaultToolkit().beep(); } catch (Throwable ignored) {}
    }
}
