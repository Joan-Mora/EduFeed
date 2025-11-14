package co.cellano.edufeed.desktop.access;

import java.util.UUID;
import co.cellano.edufeed.desktop.biometric.preview.CameraPreviewPane;
import co.cellano.edufeed.desktop.biometric.preview.FingerprintPreviewPane;
import co.cellano.edufeed.desktop.biometric.preview.PreviewControl;
import co.cellano.edufeed.desktop.biometric.preview.VoiceWaveformPane;
import co.cellano.edufeed.desktop.biometric.preview.FaceDetectorPreviewPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class BiometricCaptureView extends BorderPane {
    public interface CaptureHandler {
        void onCapture(UUID usuarioId, String modalidad);
    }

    public interface WebAuthnLauncher {
        void launch(UUID usuarioId);
    }

    private final TextField usuarioIdField = new TextField();
    private final ChoiceBox<String> modalidadBox = new ChoiceBox<>();
    private final Label statusLabel = new Label("Dispositivo: --");
    private final Button captureBtn = new Button("Capturar y verificar");
    private final Button testBtn = new Button("Probar hardware");
    private final BorderPane previewHolder = new BorderPane();
    private PreviewControl currentPreview;
    private WebAuthnLauncher webAuthnLauncher;
    // Controles de parámetros de rostro
    private HBox rostroControls;
    private TextField faceSourceField;
    private TextField scaleField;
    private Spinner<Integer> neighborsSpinner;
    private Spinner<Integer> minSizeSpinner;
    private String cascadePath;

    public BiometricCaptureView(CaptureHandler handler) {
        setPadding(new Insets(16));
        var top = new VBox(8);
        var title = new Label("Punto de acceso — Captura biométrica");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        top.getChildren().addAll(title, new Separator());
        setTop(top);

        var form = new GridPane();
        form.setHgap(8);
        form.setVgap(10);
        form.setPadding(new Insets(12));

        modalidadBox.getItems().addAll("HUELLA", "ROSTRO", "VOZ");
        modalidadBox.getSelectionModel().select("HUELLA");

        usuarioIdField.setPromptText("UUID del usuario");

        form.add(new Label("Usuario ID:"), 0, 0);
        form.add(usuarioIdField, 1, 0);
        form.add(new Label("Modalidad:"), 0, 1);
        form.add(modalidadBox, 1, 1);
        form.add(new Label("Estado de captura:"), 0, 2);
        form.add(statusLabel, 1, 2);

        var actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().addAll(testBtn, captureBtn);

        previewHolder.setPadding(new Insets(8));
        previewHolder.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6;");

        var center = new VBox(10, form, previewHolder, actions);
        center.setPadding(new Insets(8));
        setCenter(center);

        // Placeholder de estado de dispositivo
        modalidadBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            updateDeviceStatus(n);
            switchPreview(n);
        });
        updateDeviceStatus(modalidadBox.getValue());
        switchPreview(modalidadBox.getValue());

        captureBtn.setOnAction(e -> {
            try {
                UUID uid = UUID.fromString(usuarioIdField.getText().trim());
                handler.onCapture(uid, modalidadBox.getValue());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Usuario ID inválido (UUID): " + ex.getMessage(), ButtonType.OK)
                        .showAndWait();
            }
        });
    }

    public void setWebAuthnLauncher(WebAuthnLauncher launcher) {
        this.webAuthnLauncher = launcher;
    }

    public void bindHardwareTest(java.util.function.Function<String, String> tester) {
        testBtn.setOnAction(e -> {
            String modalidad = modalidadBox.getValue();
            String result = tester.apply(modalidad);
            new Alert(Alert.AlertType.INFORMATION, result, ButtonType.OK).showAndWait();
        });
    }

    public void stopPreviews() {
        if (currentPreview != null) {
            try {
                currentPreview.stop();
            } catch (Exception ignore) {
            }
        }
    }

    private void updateDeviceStatus(String modalidad) {
        switch (modalidad) {
            case "HUELLA" -> statusLabel.setText("Dispositivo: lector de huella (no detectado / demo)");
            case "ROSTRO" -> statusLabel.setText("Dispositivo: cámara (usar cámara de portátil / demo)");
            case "VOZ" -> statusLabel.setText("Dispositivo: micrófono disponible");
            default -> statusLabel.setText("Dispositivo: --");
        }
    }

    private void switchPreview(String modalidad) {
        // detener previo
        if (currentPreview != null) {
            try {
                currentPreview.stop();
            } catch (Exception ignore) {
            }
            currentPreview = null;
        }
        previewHolder.setCenter(null);
        previewHolder.setBottom(null);

        switch (modalidad) {
            case "ROSTRO" -> {
                String faceSource = System.getenv().getOrDefault("DESKTOP_FACE_SOURCE", "camera:0");
                double scale = parseDoubleOr(System.getenv().getOrDefault("DESKTOP_FACE_SCALE", "1.1"), 1.1);
                int neigh = parseIntOr(System.getenv().getOrDefault("DESKTOP_FACE_NEIGHBORS", "3"), 3);
                int minSz = parseIntOr(System.getenv().getOrDefault("DESKTOP_FACE_MINSIZE", "60"), 60);

                // Construir controles de ajuste
                rostroControls = buildRostroControls(faceSource, scale, neigh, minSz);
                previewHolder.setBottom(rostroControls);

                FaceDetectorPreviewPane p = new FaceDetectorPreviewPane(faceSource, scale, neigh, minSz, cascadePath);
                currentPreview = p;
                previewHolder.setCenter(p);
                p.start();
            }
            case "VOZ" -> {
                VoiceWaveformPane p = new VoiceWaveformPane();
                currentPreview = p;
                previewHolder.setCenter(p);
                p.start();
            }
            case "HUELLA" -> {
                FingerprintPreviewPane p = new FingerprintPreviewPane();
                currentPreview = p;
                previewHolder.setCenter(p);
                p.start();
                // Controles WebAuthn opcionales
                Button phoneBtn = new Button("Usar teléfono (WebAuthn)");
                phoneBtn.setOnAction(e -> {
                    try {
                        UUID uid = UUID.fromString(usuarioIdField.getText().trim());
                        if (webAuthnLauncher != null)
                            webAuthnLauncher.launch(uid);
                        else
                            new Alert(Alert.AlertType.INFORMATION, "Falta configurar WebAuthn en esta vista.")
                                    .showAndWait();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Usuario ID inválido (UUID): " + ex.getMessage())
                                .showAndWait();
                    }
                });
                HBox hb = new HBox(8, phoneBtn);
                hb.setPadding(new Insets(6));
                hb.setStyle("-fx-background-color:#ffffff; -fx-border-color:#dee2e6; -fx-border-width:1 0 0 0;");
                previewHolder.setBottom(hb);
            }
            default -> {
                /* nada */ }
        }
    }

    private HBox buildRostroControls(String source, double scale, int neighbors, int minSize) {
        faceSourceField = new TextField(source);
        faceSourceField.setPrefColumnCount(22);
        faceSourceField.setPromptText("camera:0 o http://ip:puerto/video");

        scaleField = new TextField(String.valueOf(scale));
        scaleField.setPrefColumnCount(4);

        neighborsSpinner = new Spinner<>(1, 20, neighbors);
        neighborsSpinner.setEditable(true);
        minSizeSpinner = new Spinner<>(20, 400, minSize);
        minSizeSpinner.setEditable(true);

        Button apply = new Button("Aplicar");
        Button browseCascade = new Button("Cargar cascade…");
        browseCascade.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("XML", "*.xml"));
            var f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                cascadePath = f.getAbsolutePath();
                // reiniciar preview con nuevo cascade
                if (currentPreview != null)
                    try {
                        currentPreview.stop();
                    } catch (Exception ignore) {
                    }
                String src = faceSourceField.getText() != null ? faceSourceField.getText().trim() : "camera:0";
                double sc = parseDoubleOr(scaleField.getText(), 1.1);
                int nb = safeSpinnerValue(neighborsSpinner, 3);
                int ms = safeSpinnerValue(minSizeSpinner, 60);
                FaceDetectorPreviewPane np = new FaceDetectorPreviewPane(src, sc, nb, ms, cascadePath);
                currentPreview = np;
                previewHolder.setCenter(np);
                np.start();
            }
        });
        apply.setOnAction(e -> {
            String src = faceSourceField.getText() != null ? faceSourceField.getText().trim() : "camera:0";
            double sc = parseDoubleOr(scaleField.getText(), 1.1);
            int nb = safeSpinnerValue(neighborsSpinner, 3);
            int ms = safeSpinnerValue(minSizeSpinner, 60);
            // reiniciar preview
            if (currentPreview != null)
                try {
                    currentPreview.stop();
                } catch (Exception ignore) {
                }
            FaceDetectorPreviewPane np = new FaceDetectorPreviewPane(src, sc, nb, ms);
            currentPreview = np;
            previewHolder.setCenter(np);
            np.start();
        });

        HBox hb = new HBox(8,
                new Label("Fuente:"), faceSourceField,
                new Label("scale:"), scaleField,
                new Label("neighbors:"), neighborsSpinner,
                new Label("minSize:"), minSizeSpinner,
                apply,
                browseCascade);
        hb.setPadding(new Insets(6));
        hb.setStyle("-fx-background-color:#ffffff; -fx-border-color:#dee2e6; -fx-border-width:1 0 0 0;");
        return hb;
    }

    private static double parseDoubleOr(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static int safeSpinnerValue(Spinner<Integer> sp, int def) {
        try {
            sp.increment(0);
            return sp.getValue();
        } catch (Exception e) {
            return def;
        }
    }
}
