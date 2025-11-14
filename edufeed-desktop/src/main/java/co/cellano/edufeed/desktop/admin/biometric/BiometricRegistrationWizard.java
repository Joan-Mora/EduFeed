package co.cellano.edufeed.desktop.admin.biometric;

import co.cellano.edufeed.desktop.service.BiometricSessionService;
import co.cellano.edufeed.desktop.util.QRCodeGenerator;
import com.google.zxing.WriterException;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wizard para registro biométrico completo.
 * Guía al usuario a través de los 3 pasos: Huella, Rostro y Voz.
 */
public class BiometricRegistrationWizard extends Stage {

    private final String userId;
    private final String userName;
    private final String baseUrl;
    private final BiometricSessionService sessionService;
    private String sessionId;
    private String token;

    // Componentes UI
    private Label lblTitulo;
    private Label lblInstrucciones;
    private ImageView qrImageView;
    private Label lblUrlDebug;
    private Label lblEstado;
    private ProgressBar progressBar;
    private Label lblProgreso;
    private Button btnContinuar;
    private Button btnReintentar;
    private Button btnSaltar;
    private Button btnCancelar;
    private VBox statusContainer;

    // Estado del wizard
    private int currentStep = 0; // 0=Huella, 1=Rostro, 2=Voz
    private static final String[] STEP_NAMES = { "Huella Digital", "Reconocimiento Facial", "Reconocimiento de Voz" };
    private static final String[] STEP_ICONS = { "🖐️", "📸", "🎤" };
    private static final String[] MODALITY_NAMES = { "fingerprint", "face", "voice" };

    // Polling
    private ScheduledExecutorService pollingExecutor;
    private boolean[] completedSteps = new boolean[3];

    public BiometricRegistrationWizard(String userId, String userName, String baseUrl) {
        this.userId = userId;
        this.userName = userName;
        this.baseUrl = baseUrl;
        this.sessionService = new BiometricSessionService(baseUrl);

        initStyle(StageStyle.DECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Registro Biométrico - " + userName);
        setResizable(false);

        initializeUI();
        startSession();
    }

    private void initializeUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: white;");

        // Header
        VBox header = createHeader();

        // QR Code
        VBox qrContainer = createQRContainer();

        // Estado
        statusContainer = createStatusContainer();

        // Progress
        VBox progressContainer = createProgressContainer();

        // Botones
        HBox buttonBar = createButtonBar();

        root.getChildren().addAll(header, qrContainer, statusContainer, progressContainer, buttonBar);

        // Envolver en ScrollPane para que los botones siempre sean accesibles
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-background: white;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Scene scene = new Scene(scrollPane, 600, 750);
        setScene(scene);
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label lblIcon = new Label("🔐");
        lblIcon.setFont(Font.font(48));

        lblTitulo = new Label();
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        lblTitulo.setTextFill(Color.web("#2d3748"));

        lblInstrucciones = new Label();
        lblInstrucciones.setFont(Font.font(14));
        lblInstrucciones.setTextFill(Color.web("#4a5568"));
        lblInstrucciones.setWrapText(true);
        lblInstrucciones.setMaxWidth(500);
        lblInstrucciones.setAlignment(Pos.CENTER);
        lblInstrucciones.setStyle("-fx-text-alignment: center;");

        header.getChildren().addAll(lblIcon, lblTitulo, lblInstrucciones);
        return header;
    }

    private VBox createQRContainer() {
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));
        container.setStyle(
                "-fx-background-color: #f7fafc;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 15;" +
                        "-fx-background-radius: 15;");

        Label lblQRTitle = new Label("📱 Escanea este código QR");
        lblQRTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        qrImageView = new ImageView();
        qrImageView.setFitWidth(300);
        qrImageView.setFitHeight(300);
        qrImageView.setPreserveRatio(true);

        Label lblQRHelp = new Label("Usa tu dispositivo móvil para escanear el código");
        lblQRHelp.setFont(Font.font(12));
        lblQRHelp.setTextFill(Color.web("#718096"));

        // Debug: mostrar la modalidad/URL generada del QR para verificar que cambia
        // entre pasos
        lblUrlDebug = new Label("");
        lblUrlDebug.setFont(Font.font(11));
        lblUrlDebug.setTextFill(Color.web("#A0AEC0"));
        lblUrlDebug.setWrapText(true);
        lblUrlDebug.setMaxWidth(520);

        container.getChildren().addAll(lblQRTitle, qrImageView, lblQRHelp, lblUrlDebug);
        return container;
    }

    private VBox createStatusContainer() {
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(15));
        container.setStyle(
                "-fx-background-color: #bee3f8;" +
                        "-fx-border-color: #4299e1;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;");

        lblEstado = new Label("⏳ Esperando registro...");
        lblEstado.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblEstado.setTextFill(Color.web("#2c5282"));

        container.getChildren().add(lblEstado);
        return container;
    }

    private VBox createProgressContainer() {
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #667eea;");

        lblProgreso = new Label("Paso 1 de 3");
        lblProgreso.setFont(Font.font(12));
        lblProgreso.setTextFill(Color.web("#4a5568"));

        container.getChildren().addAll(progressBar, lblProgreso);
        return container;
    }

    private HBox createButtonBar() {
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER);

        btnContinuar = new Button("➡️ Continuar");
        btnContinuar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 25;");
        btnContinuar.setOnAction(e -> nextStep());
        btnContinuar.setDisable(true);

        btnReintentar = new Button("🔄 Reintentar");
        btnReintentar.setStyle(
                "-fx-background-color: #fbbf24;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 25;");
        btnReintentar.setOnAction(e -> retryStep());
        btnReintentar.setVisible(false);
        btnReintentar.setManaged(false); // No ocupa espacio cuando está oculto

        btnSaltar = new Button("⏭️ Saltar");
        btnSaltar.setStyle(
                "-fx-background-color: #9ca3af;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 25;");
        btnSaltar.setOnAction(e -> skipStep());
        btnSaltar.setVisible(false);
        btnSaltar.setManaged(false); // No ocupa espacio cuando está oculto

        btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setStyle(
                "-fx-background-color: #ef4444;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 12 30;" +
                        "-fx-background-radius: 25;");
        btnCancelar.setOnAction(e -> cancelWizard());

        // Orden: Continuar (habilitado cuando completa) y Cancelar siempre visible
        buttonBar.getChildren().addAll(btnContinuar, btnCancelar, btnReintentar, btnSaltar);
        return buttonBar;
    }

    private void startSession() {
        new Thread(() -> {
            try {
                // Generar token y crear sesión
                token = BiometricSessionService.generateToken();
                sessionId = sessionService.startSession(userId, token);

                if (sessionId == null || sessionId.isBlank()) {
                    throw new IllegalStateException("Backend no devolvió sessionId");
                }

                Platform.runLater(() -> {
                    updateStep();
                    startPolling();
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Error al iniciar sesión: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            }
        }).start();
    }

    private void updateStep() {
        if (currentStep >= 3) {
            finishWizard();
            return;
        }

        // Actualizar UI
        String stepName = STEP_NAMES[currentStep];
        String stepIcon = STEP_ICONS[currentStep];
        String modalityName = MODALITY_NAMES[currentStep];

        lblTitulo.setText(stepIcon + " Paso " + (currentStep + 1) + ": " + stepName);
        lblInstrucciones
                .setText("Escanea el código QR con tu dispositivo móvil para registrar tu " + stepName.toLowerCase());
        lblProgreso.setText("Paso " + (currentStep + 1) + " de 3");
        progressBar.setProgress((double) currentStep / 3);

        // Generar nuevo QR
        generateQRCode(modalityName);
    }

    private void generateQRCode(String modalityName) {
        new Thread(() -> {
            try {
                String url = QRCodeGenerator.buildModalityUrl(baseUrl, userId, token, sessionId, modalityName);
                if (sessionId == null) {
                    throw new IllegalStateException("Sesión no iniciada todavía");
                }
                BufferedImage qrImage = QRCodeGenerator.generateQRCode(url, 300, 300);
                Image fxImage = SwingFXUtils.toFXImage(qrImage, null);

                // Log y UI debug para confirmar que la URL cambia según la modalidad
                System.out.println("[BiometricWizard] Generado QR para modalidad=" + modalityName + " URL=" + url);

                Platform.runLater(() -> {
                    qrImageView.setImage(fxImage);
                    // Mostrar solo la parte de la ruta para que sea legible
                    try {
                        String pathOnly = url.replace(baseUrl, "");
                        lblUrlDebug.setText("Destino: " + pathOnly);
                    } catch (Exception ignore) {
                        lblUrlDebug.setText(url);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showError("Error al generar QR: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
            }
        }).start();
    }

    private void startPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdown();
        }

        pollingExecutor = Executors.newSingleThreadScheduledExecutor();
        pollingExecutor.scheduleAtFixedRate(() -> {
            try {
                BiometricSessionService.RegistrationStatus status = sessionService.getStatus(userId, sessionId);

                Platform.runLater(() -> {
                    // Actualizar estado
                    completedSteps[0] = status.huellaCompletada;
                    completedSteps[1] = status.rostroCompletado;
                    completedSteps[2] = status.vozCompletada;

                    // Si el paso actual está completado, habilitar continuar
                    if (completedSteps[currentStep]) {
                        lblEstado.setText("✅ ¡Registro completado!");
                        statusContainer.setStyle(
                                "-fx-background-color: #c6f6d5;" +
                                        "-fx-border-color: #48bb78;" +
                                        "-fx-border-width: 2;" +
                                        "-fx-border-radius: 10;" +
                                        "-fx-background-radius: 10;");
                        btnContinuar.setDisable(false);
                        btnReintentar.setVisible(false);
                        btnReintentar.setManaged(false);
                    } else {
                        lblEstado.setText("⏳ Esperando registro en dispositivo móvil...");
                    }
                });

            } catch (Exception e) {
                System.err.println("Error en polling: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void nextStep() {
        currentStep++;
        btnContinuar.setDisable(true);
        updateStep();
    }

    private void retryStep() {
        btnContinuar.setDisable(true);
        btnReintentar.setVisible(false);
        btnReintentar.setManaged(false);
        btnSaltar.setVisible(false);
        btnSaltar.setManaged(false);
        updateStep();
    }

    private void skipStep() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar");
        alert.setHeaderText("¿Saltar este paso?");
        alert.setContentText("Podrás registrar esta modalidad más tarde desde el menú de usuarios.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            nextStep();
        }
    }

    private void finishWizard() {
        stopPolling();

        int completed = 0;
        for (boolean step : completedSteps) {
            if (step)
                completed++;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registro Completado");
        alert.setHeaderText("Proceso finalizado");
        alert.setContentText(String.format("Se completaron %d de 3 modalidades biométricas.\n\n" +
                "✅ Huella: %s\n" +
                "✅ Rostro: %s\n" +
                "✅ Voz: %s",
                completed,
                completedSteps[0] ? "Registrada" : "Pendiente",
                completedSteps[1] ? "Registrado" : "Pendiente",
                completedSteps[2] ? "Registrada" : "Pendiente"));

        alert.showAndWait();
        close();
    }

    private void cancelWizard() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar");
        alert.setHeaderText("¿Cancelar registro?");
        alert.setContentText("Se perderá el progreso actual.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            stopPolling();
            close();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error en el registro");
        alert.setContentText(message);
        alert.showAndWait();

        btnReintentar.setVisible(true);
        btnReintentar.setManaged(true);
        btnSaltar.setVisible(true);
        btnSaltar.setManaged(true);
    }

    private void stopPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdown();
        }
    }

    @Override
    public void close() {
        stopPolling();
        super.close();
    }
}
