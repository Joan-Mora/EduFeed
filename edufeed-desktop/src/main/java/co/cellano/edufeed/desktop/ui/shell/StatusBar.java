package co.cellano.edufeed.desktop.ui.shell;

import co.cellano.edufeed.desktop.session.SessionContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Barra de estado inferior con información del sistema:
 * - Estado de conexión
 * - Usuario actual
 * - Información del ambiente
 * - Versión de la aplicación
 */
public class StatusBar extends HBox {

    private final Label statusLabel;
    private final Label userLabel;
    private final Label versionLabel;
    private final Label timeLabel;

    public StatusBar() {
        getStyleClass().add("app-statusbar");
        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(6, 16, 6, 16));

        // Estado de conexión
        statusLabel = new Label("● Conectado");
        statusLabel.getStyleClass().addAll("app-statusbar__item", "app-statusbar__status--connected");

        // Separador
        Region sep1 = createSeparator();

        // Usuario actual
        userLabel = new Label("Usuario: " + (SessionContext.username != null ? SessionContext.username : "N/A"));
        userLabel.getStyleClass().add("app-statusbar__item");

        // Separador
        Region sep2 = createSeparator();

        // Spacer para empujar versión a la derecha
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Hora actual
        timeLabel = new Label(getCurrentTime());
        timeLabel.getStyleClass().add("app-statusbar__item");

        // Separador
        Region sep3 = createSeparator();

        // Versión
        versionLabel = new Label("v2.0.0");
        versionLabel.getStyleClass().add("app-statusbar__item");

        getChildren().addAll(
                statusLabel, sep1,
                userLabel, sep2,
                spacer,
                timeLabel, sep3,
                versionLabel);

        // Actualizar hora cada minuto
        startTimeUpdate();
    }

    /**
     * Establece el texto del estado
     */
    public void setText(String text) {
        statusLabel.setText(text);
    }

    /**
     * Actualiza el estado de conexión
     */
    public void setConnectionStatus(boolean connected) {
        statusLabel.getStyleClass().removeAll("app-statusbar__status--connected",
                "app-statusbar__status--disconnected");
        if (connected) {
            statusLabel.setText("● Conectado");
            statusLabel.getStyleClass().add("app-statusbar__status--connected");
        } else {
            statusLabel.setText("● Desconectado");
            statusLabel.getStyleClass().add("app-statusbar__status--disconnected");
        }
    }

    /**
     * Actualiza el usuario mostrado
     */
    public void setUser(String username) {
        userLabel.setText("Usuario: " + username);
    }

    private Region createSeparator() {
        Region sep = new Region();
        sep.getStyleClass().add("app-statusbar__separator");
        sep.setMinWidth(1);
        sep.setMaxWidth(1);
        sep.setMinHeight(12);
        return sep;
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void startTimeUpdate() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(60),
                        e -> timeLabel.setText(getCurrentTime())));
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
}
