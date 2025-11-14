package co.cellano.edufeed.desktop.modules;

import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dashboard embebible con 4 KPIs y 2 sparklines simples.
 * Anima los contadores y dibuja líneas en Canvas sin dependencias externas.
 */
public class DashboardModule {
    private final BorderPane root = new BorderPane();
    private final String baseUrl;
    private final String bearer;
    private final Set<String> roles;

    public DashboardModule(String baseUrl, String bearer, Set<String> roles) {
        this.baseUrl = baseUrl;
        this.bearer = bearer;
        this.roles = roles;
        buildUI();
    }

    public Node getView() {
        AnimationUtils.fadeIn(root, AnimationUtils.NORMAL);
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24));

        // Header
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700;");
        Label subtitle = new Label("Bienvenido, " + primaryRole(roles));
        subtitle.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");
        HBox header = new HBox(16, title, subtitle);
        header.setAlignment(Pos.BASELINE_LEFT);
        root.setTop(header);

        // KPIs grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(16, 0, 0, 0));

        KpiCard k1 = new KpiCard("Ingresos Hoy", "personas", Color.web("#10B981"));
        KpiCard k2 = new KpiCard("Asistencias", "registros", Color.web("#3B82F6"));
        KpiCard k3 = new KpiCard("Pagos", "transacciones", Color.web("#F59E0B"));
        KpiCard k4 = new KpiCard("Rechazos", "eventos", Color.web("#EF4444"));

        grid.add(k1, 0, 0);
        grid.add(k2, 1, 0);
        grid.add(k3, 2, 0);
        grid.add(k4, 3, 0);

        // Simular datos con animaciones escalonadas (efecto cascada)
        Random r = new Random();
        int v1 = 50 + r.nextInt(150);
        int v2 = 300 + r.nextInt(200);
        int v3 = 40 + r.nextInt(90);
        int v4 = 5 + r.nextInt(30);

        // Animar con delays escalonados para efecto cascada
        javafx.application.Platform.runLater(() -> {
            k1.animateTo(v1);
            javafx.animation.PauseTransition p1 = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(120));
            p1.setOnFinished(e1 -> {
                k2.animateTo(v2);
                javafx.animation.PauseTransition p2 = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(120));
                p2.setOnFinished(e2 -> {
                    k3.animateTo(v3);
                    javafx.animation.PauseTransition p3 = new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(120));
                    p3.setOnFinished(e3 -> k4.animateTo(v4));
                    p3.play();
                });
                p2.play();
            });
            p1.play();
        });

        // Sparklines panel
        HBox charts = new HBox(16,
                new SparkCard("Tendencia de Ingresos", sampleSeries(24), Color.web("#10B981")),
                new SparkCard("Tendencia de Pagos", sampleSeries(24), Color.web("#F59E0B")));
        charts.setPadding(new Insets(8, 0, 0, 0));

        BorderPane center = new BorderPane();
        center.setTop(grid);
        center.setCenter(charts);
        root.setCenter(center);
    }

    private List<Double> sampleSeries(int n) {
        Random r = new Random();
        List<Double> data = new ArrayList<>(n);
        double v = 50 + r.nextDouble() * 20;
        for (int i = 0; i < n; i++) {
            v += r.nextGaussian() * 3;
            if (v < 0)
                v = 5;
            data.add(v);
        }
        return data;
    }

    private String primaryRole(Set<String> roles) {
        if (roles == null)
            return "Usuario";
        if (roles.contains("ROLE_ADMIN"))
            return "Administrador";
        if (roles.contains("ROLE_SUPERVISOR"))
            return "Supervisor";
        if (roles.contains("ROLE_OPERADOR_CAJA"))
            return "Operador de Caja";
        if (roles.contains("ROLE_OPERADOR_ACCESO"))
            return "Operador de Acceso";
        return "Usuario";
    }

    // --- UI components ---
    static class KpiCard extends StackPane {
        private final Label title = new Label();
        private final Label value = new Label("0");
        private final Label unit = new Label();

        KpiCard(String titleText, String unitText, Color color) {
            setPadding(new Insets(16));
            getStyleClass().add("app-card");
            setStyle(
                    "-fx-background-color: -fx-surface; -fx-background-radius: 12; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");

            Rectangle accent = new Rectangle(6, 32, color);
            accent.setArcWidth(6);
            accent.setArcHeight(6);

            title.setText(titleText);
            title.setStyle("-fx-text-fill: -fx-text; -fx-font-size: 12px;");

            value.setFont(Font.font(28));
            value.setStyle("-fx-text-fill: -fx-text;");

            unit.setText(unitText);
            unit.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

            HBox box = new HBox(12, accent, new VBox(title, value, unit));
            getChildren().add(box);
            AnimationUtils.scaleIn(this, AnimationUtils.FAST);
        }

        void animateTo(int target) {
            AnimationUtils.animateNumber(value, 0, target, AnimationUtils.NORMAL);
            AnimationUtils.pulse(this);
        }
    }

    static class SparkCard extends BorderPane {
        SparkCard(String title, List<Double> data, Color color) {
            setPadding(new Insets(16));
            setStyle(
                    "-fx-background-color: -fx-surface; -fx-background-radius: 12; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");
            Label t = new Label(title);
            t.setStyle("-fx-text-fill: -fx-text; -fx-font-size: 12px; -fx-font-weight: 600;");
            setTop(t);
            Canvas c = new Canvas(480, 160);
            drawSparkline(c.getGraphicsContext2D(), data, color);
            setCenter(c);
            AnimationUtils.fadeIn(this, AnimationUtils.MEDIUM);
        }

        private void drawSparkline(GraphicsContext g, List<Double> data, Color color) {
            double w = g.getCanvas().getWidth();
            double h = g.getCanvas().getHeight();
            g.setFill(Color.web("#F3F4F6"));
            g.fillRoundRect(0, 0, w, h, 12, 12);
            if (data == null || data.isEmpty())
                return;
            double min = data.stream().min(Double::compareTo).orElse(0.0);
            double max = data.stream().max(Double::compareTo).orElse(1.0);
            double range = Math.max(1e-6, max - min);
            g.setStroke(color);
            g.setLineWidth(2.0);
            double step = w / (data.size() - 1);
            for (int i = 1; i < data.size(); i++) {
                double x1 = (i - 1) * step;
                double y1 = h - ((data.get(i - 1) - min) / range) * (h - 12) - 6;
                double x2 = i * step;
                double y2 = h - ((data.get(i) - min) / range) * (h - 12) - 6;
                g.strokeLine(x1, y1, x2, y2);
            }
        }
    }
}
