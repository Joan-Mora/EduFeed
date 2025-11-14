package co.cellano.edufeed.desktop.modules;

import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Módulo de Control de Acceso V2 - Rediseñado con QR y autenticación biométrica
 * móvil
 * 
 * Flujo:
 * 1. Usuario ingresa ID o Cédula
 * 2. Se genera código QR automáticamente
 * 3. QR abre página móvil con opciones: Huella, FaceID, Voz
 * 4. Página móvil valida biometría y envía resultado
 * 5. Desktop muestra información de pagos y permite finalizar sesión
 */
public class AccessCheckModuleV2 {
    private final BorderPane root = new BorderPane();
    private final String baseUrl;
    private final String bearer;

    private TextField idField;
    private ImageView qrImageView;
    private VBox qrContainer;
    private VBox userInfoContainer;
    private Label statusLabel;

    private String currentUserId;

    public AccessCheckModuleV2(String baseUrl, String bearer) {
        this.baseUrl = baseUrl;
        this.bearer = bearer;
        buildUI();
    }

    public Node getView() {
        AnimationUtils.fadeIn(root, AnimationUtils.NORMAL);
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: -fx-background;");

        // Header
        Label title = new Label("Control de Acceso");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: -fx-text;");

        Label subtitle = new Label("Ingrese el ID o Cédula del usuario para generar el código QR de autenticación");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text-secondary;");
        subtitle.setWrapText(true);

        VBox header = new VBox(8, title, subtitle);
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        // Contenido principal
        VBox content = new VBox(32);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(40, 0, 0, 0));

        // Icono de persona grande
        Label personIcon = new Label("👤");
        personIcon.setStyle("-fx-font-size: 120px;");
        AnimationUtils.pulse(personIcon);

        // Campo de entrada de ID/Cédula
        VBox inputSection = new VBox(12);
        inputSection.setAlignment(Pos.CENTER);
        inputSection.setMaxWidth(500);

        Label inputLabel = new Label("ID de Usuario o Cédula");
        inputLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: -fx-text;");

        idField = new TextField();
        idField.setPromptText("Ej: 1234567890 o user@123");
        idField.setStyle(
                "-fx-font-size: 18px; -fx-padding: 16 24; -fx-background-radius: 12; " +
                        "-fx-border-color: -fx-border; -fx-border-width: 2; -fx-border-radius: 12; " +
                        "-fx-background-color: white;");
        idField.setPrefWidth(450);

        // Efecto focus
        idField.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (nowFocused) {
                idField.setStyle(
                        "-fx-font-size: 18px; -fx-padding: 16 24; -fx-background-radius: 12; " +
                                "-fx-border-color: -fx-primary; -fx-border-width: 2; -fx-border-radius: 12; " +
                                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, -fx-primary, 8, 0.3, 0, 0);");
            } else {
                idField.setStyle(
                        "-fx-font-size: 18px; -fx-padding: 16 24; -fx-background-radius: 12; " +
                                "-fx-border-color: -fx-border; -fx-border-width: 2; -fx-border-radius: 12; " +
                                "-fx-background-color: white;");
            }
        });

        idField.setOnAction(e -> generateQRCode());

        Button generateBtn = new Button("Generar Código QR");
        generateBtn.setStyle(
                "-fx-background-color: -fx-primary; -fx-text-fill: white; " +
                        "-fx-font-size: 16px; -fx-font-weight: 700; -fx-padding: 14 32; " +
                        "-fx-background-radius: 12; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 4);");
        generateBtn.setOnAction(e -> generateQRCode());
        AnimationUtils.setupHoverEffect(generateBtn);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-error;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(450);
        statusLabel.setAlignment(Pos.CENTER);

        inputSection.getChildren().addAll(inputLabel, idField, generateBtn, statusLabel);

        // Contenedor del QR (inicialmente oculto)
        qrContainer = new VBox(16);
        qrContainer.setAlignment(Pos.CENTER);
        qrContainer.setVisible(false);
        qrContainer.setManaged(false);

        Label qrTitle = new Label("Escanea este código QR con tu móvil");
        qrTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -fx-text;");

        VBox qrCard = new VBox(16);
        qrCard.setAlignment(Pos.CENTER);
        qrCard.setPadding(new Insets(24));
        qrCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 16, 0, 0, 4);");

        qrImageView = new ImageView();
        qrImageView.setFitWidth(300);
        qrImageView.setFitHeight(300);
        qrImageView.setPreserveRatio(true);

        Label qrInstructions = new Label(
                "Selecciona tu método de autenticación:\n• Huella Digital\n• FaceID\n• Reconocimiento de Voz");
        qrInstructions.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-text-secondary; -fx-text-alignment: center;");
        qrInstructions.setWrapText(true);

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: -fx-error; " +
                        "-fx-font-size: 14px; -fx-padding: 8 16; -fx-cursor: hand; " +
                        "-fx-border-color: -fx-error; -fx-border-width: 1; -fx-border-radius: 8;");
        cancelBtn.setOnAction(e -> resetToInput());

        qrCard.getChildren().addAll(qrImageView, qrInstructions);
        qrContainer.getChildren().addAll(qrTitle, qrCard, cancelBtn);

        // Contenedor de información del usuario (inicialmente oculto)
        userInfoContainer = new VBox(20);
        userInfoContainer.setAlignment(Pos.TOP_CENTER);
        userInfoContainer.setVisible(false);
        userInfoContainer.setManaged(false);

        content.getChildren().addAll(personIcon, inputSection, qrContainer, userInfoContainer);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        root.setCenter(scroll);
    }

    private void generateQRCode() {
        String userId = idField.getText().trim();
        if (userId.isEmpty()) {
            showError("Por favor ingrese un ID o Cédula");
            AnimationUtils.shake(idField);
            return;
        }

        currentUserId = userId;
        clearError();

        // URL que se codificará en el QR - apunta a la página web de autenticación
        String authUrl = baseUrl + "/api/auth/biometric?userId=" + userId + "&token=" + bearer;

        try {
            // Generar QR Code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(authUrl, BarcodeFormat.QR_CODE, 300, 300);

            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Convertir BufferedImage a JavaFX Image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            Image image = new Image(new ByteArrayInputStream(baos.toByteArray()));

            qrImageView.setImage(image);

            // Mostrar QR con animación
            qrContainer.setVisible(true);
            qrContainer.setManaged(true);
            AnimationUtils.fadeIn(qrContainer, AnimationUtils.MEDIUM);
            AnimationUtils.scaleIn(qrContainer, AnimationUtils.MEDIUM);

            // Simular espera de autenticación (en producción, esto vendría del backend vía
            // WebSocket)
            simulateAuthenticationFlow();

        } catch (WriterException | IOException e) {
            showError("Error al generar el código QR: " + e.getMessage());
            AnimationUtils.shake(statusLabel);
        }
    }

    private void simulateAuthenticationFlow() {
        // Polling al backend para verificar si el usuario se autenticó desde el móvil
        Thread pollThread = new Thread(() -> {
            try {
                int attempts = 0;
                int maxAttempts = 60; // 60 intentos = 2 minutos (polling cada 2 segundos)

                while (attempts < maxAttempts) {
                    Thread.sleep(2000); // Esperar 2 segundos entre cada intento

                    // Hacer petición al backend
                    String url = baseUrl + "/api/auth/biometric/status/" + currentUserId;

                    try {
                        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(url))
                                .header("Authorization", "Bearer " + bearer)
                                .GET()
                                .build();

                        java.net.http.HttpResponse<String> response = client.send(request,
                                java.net.http.HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            // Parsear respuesta JSON (simple parsing manual)
                            String body = response.body();

                            if (body.contains("\"authenticated\":true")) {
                                // Autenticación exitosa
                                Platform.runLater(() -> {
                                    showUserInfo(body);
                                });
                                return; // Salir del polling
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("Error en polling: " + e.getMessage());
                    }

                    attempts++;
                }

                // Si llega aquí, se acabó el tiempo
                Platform.runLater(() -> {
                    showError("Tiempo de espera agotado. Por favor, intenta nuevamente.");
                    resetToInput();
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void showUserInfo(String jsonResponse) {
        // Ocultar QR
        qrContainer.setVisible(false);
        qrContainer.setManaged(false);

        // Parsear datos del JSON (simple parsing manual)
        String nombre = extractJsonValue(jsonResponse, "nombre");
        String cedula = extractJsonValue(jsonResponse, "cedula");
        String sueldo = extractJsonValue(jsonResponse, "sueldo");
        String pagosPendientes = extractJsonValue(jsonResponse, "pagosPendientes");
        String pagosRealizados = extractJsonValue(jsonResponse, "pagosRealizados");
        String pagosProgramados = extractJsonValue(jsonResponse, "pagosProgramados");

        // Construir información del usuario
        userInfoContainer.getChildren().clear();

        // Tarjeta de bienvenida
        VBox welcomeCard = new VBox(12);
        welcomeCard.setPadding(new Insets(24));
        welcomeCard.setAlignment(Pos.CENTER);
        welcomeCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 16, 0, 0, 4);");
        welcomeCard.setMaxWidth(600);

        Label welcomeLabel = new Label("✅ Acceso Autorizado");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: -fx-success;");

        Label userName = new Label(nombre.isEmpty() ? "Usuario" : nombre);
        userName.setStyle("-fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: -fx-text;");

        Label userCedula = new Label("Cédula: " + (cedula.isEmpty() ? currentUserId : cedula));
        userCedula.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text-secondary;");

        Label userSalary = new Label("Sueldo: $" + (sueldo.isEmpty() ? "0" : sueldo));
        userSalary.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: -fx-text;");

        welcomeCard.getChildren().addAll(welcomeLabel, userName, userCedula, userSalary);

        // Grid de información de pagos
        GridPane paymentsGrid = new GridPane();
        paymentsGrid.setHgap(16);
        paymentsGrid.setVgap(16);
        paymentsGrid.setAlignment(Pos.CENTER);
        paymentsGrid.setMaxWidth(600);

        // Pagos Pendientes
        VBox pendingBox = createPaymentCard("Pagos Pendientes", pagosPendientes.isEmpty() ? "0" : pagosPendientes, "💰",
                Color.web("#F59E0B"));
        GridPane.setConstraints(pendingBox, 0, 0);

        // Pagos Realizados
        VBox completedBox = createPaymentCard("Pagos Realizados", pagosRealizados.isEmpty() ? "0" : pagosRealizados,
                "✅", Color.web("#10B981"));
        GridPane.setConstraints(completedBox, 1, 0);

        // Pagos Programados
        VBox scheduledBox = createPaymentCard("Pagos Programados", pagosProgramados.isEmpty() ? "0" : pagosProgramados,
                "📅", Color.web("#3B82F6"));
        GridPane.setConstraints(scheduledBox, 0, 1, 2, 1);

        paymentsGrid.getChildren().addAll(pendingBox, completedBox, scheduledBox);

        // Botón finalizar sesión
        Button finishBtn = new Button("Finalizar Sesión");
        finishBtn.setStyle(
                "-fx-background-color: -fx-primary; -fx-text-fill: white; " +
                        "-fx-font-size: 16px; -fx-font-weight: 700; -fx-padding: 14 32; " +
                        "-fx-background-radius: 12; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 4);");
        finishBtn.setOnAction(e -> resetToInput());
        AnimationUtils.setupHoverEffect(finishBtn);

        userInfoContainer.getChildren().addAll(welcomeCard, paymentsGrid, finishBtn);
        userInfoContainer.setVisible(true);
        userInfoContainer.setManaged(true);
        AnimationUtils.fadeIn(userInfoContainer, AnimationUtils.MEDIUM);
    }

    private VBox createPaymentCard(String title, String value, String icon, Color accentColor) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setMinWidth(280);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 40px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: " + toHexString(accentColor) + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text-secondary;");

        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        AnimationUtils.scaleIn(card, AnimationUtils.NORMAL);

        return card;
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void resetToInput() {
        userInfoContainer.setVisible(false);
        userInfoContainer.setManaged(false);
        qrContainer.setVisible(false);
        qrContainer.setManaged(false);
        idField.clear();
        currentUserId = null;
        clearError();
        AnimationUtils.pulse(idField);
    }

    private void showError(String message) {
        statusLabel.setText("❌ " + message);
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-error; -fx-font-weight: 600;");
    }

    private void clearError() {
        statusLabel.setText("");
    }

    /**
     * Extrae un valor de un JSON simple (sin usar librerías externas)
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);

            if (startIndex == -1) {
                return "";
            }

            startIndex += searchKey.length();

            // Saltar espacios
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }

            // Si es una cadena (empieza con ")
            if (json.charAt(startIndex) == '"') {
                startIndex++;
                int endIndex = json.indexOf('"', startIndex);
                return json.substring(startIndex, endIndex);
            }

            // Si es un número o booleano
            int endIndex = startIndex;
            while (endIndex < json.length() &&
                    (Character.isDigit(json.charAt(endIndex)) ||
                            json.charAt(endIndex) == '.' ||
                            json.charAt(endIndex) == '-' ||
                            Character.isLetter(json.charAt(endIndex)))) {
                endIndex++;
            }

            return json.substring(startIndex, endIndex).trim();

        } catch (Exception e) {
            return "";
        }
    }
}
