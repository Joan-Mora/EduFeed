package co.cellano.edufeed.desktop.access;

import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class BiometricCaptureView extends BorderPane {
    public interface CaptureHandler {
        void onCapture(UUID usuarioId, String modalidad);
    }

    private final TextField usuarioIdField = new TextField();
    private final ChoiceBox<String> modalidadBox = new ChoiceBox<>();
    private final Label statusLabel = new Label("Dispositivo: --");
    private final Button captureBtn = new Button("Capturar y verificar");
    private final Button testBtn = new Button("Probar hardware");

    public BiometricCaptureView(CaptureHandler handler) {
        setPadding(new Insets(16));
        var top = new VBox(8);
        var title = new Label("Punto de acceso — Captura biométrica");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        top.getChildren().addAll(title, new Separator());
        setTop(top);

        var form = new GridPane();
        form.setHgap(8); form.setVgap(10); form.setPadding(new Insets(12));

        modalidadBox.getItems().addAll("HUELLA","ROSTRO","VOZ");
        modalidadBox.getSelectionModel().select("HUELLA");

        usuarioIdField.setPromptText("UUID del usuario");

        form.add(new Label("Usuario ID:"), 0, 0); form.add(usuarioIdField, 1, 0);
        form.add(new Label("Modalidad:"), 0, 1); form.add(modalidadBox, 1, 1);
        form.add(new Label("Estado de captura:"), 0, 2); form.add(statusLabel, 1, 2);

        var actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
    actions.getChildren().addAll(testBtn, captureBtn);

        var center = new VBox(10, form, actions);
        center.setPadding(new Insets(8));
        setCenter(center);

        // Placeholder de estado de dispositivo
        modalidadBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateDeviceStatus(n));
        updateDeviceStatus(modalidadBox.getValue());

        captureBtn.setOnAction(e -> {
            try {
                UUID uid = UUID.fromString(usuarioIdField.getText().trim());
                handler.onCapture(uid, modalidadBox.getValue());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Usuario ID inválido (UUID): " + ex.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    public void bindHardwareTest(java.util.function.Function<String, String> tester) {
        testBtn.setOnAction(e -> {
            String modalidad = modalidadBox.getValue();
            String result = tester.apply(modalidad);
            new Alert(Alert.AlertType.INFORMATION, result, ButtonType.OK).showAndWait();
        });
    }

    private void updateDeviceStatus(String modalidad) {
        switch (modalidad) {
            case "HUELLA" -> statusLabel.setText("Dispositivo: lector de huella (no detectado / demo)");
            case "ROSTRO" -> statusLabel.setText("Dispositivo: cámara (usar cámara de portátil / demo)");
            case "VOZ" -> statusLabel.setText("Dispositivo: micrófono disponible");
            default -> statusLabel.setText("Dispositivo: --");
        }
    }
}
