package co.cellano.edufeed.desktop.access;

import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView extends StackPane {
    public interface LoginHandler {
        void onLogin(String username, String password);
    }

    private final TextField user = new TextField();
    private final PasswordField pass = new PasswordField();
    private final TextField passVisible = new TextField();
    private final ToggleButton showPass = new ToggleButton("👁 Mostrar contraseña");
    private final Button loginBtn = new Button("Iniciar sesión");
    private final Label status = new Label();
    private final VBox card = new VBox(14);

    public LoginView(LoginHandler handler) {
        // Fondo fullscreen con degradado vibrante que contrasta con la card blanca
        setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #4F46E5 0%, #7C3AED 30%, #EC4899 60%, #F43F5E 100%);");
        setPadding(new Insets(24));

        // Tarjeta central con múltiples capas de sombra para mayor profundidad
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(40));
        card.setMaxWidth(460);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.98), rgba(255,255,255,0.95)); "
                        + "-fx-background-radius: 24; "
                        + "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.4), 32, 0.4, 0, 12), "
                        + "dropshadow(gaussian, rgba(0, 0, 0, 0.2), 16, 0.3, 0, 4), "
                        + "innershadow(gaussian, rgba(255, 255, 255, 0.8), 2, 0.5, 0, 1); "
                        + "-fx-border-color: rgba(255,255,255,0.5); -fx-border-width: 1; -fx-border-radius: 24;");

        Label logo = new Label("🎓 EduFeed");
        logo.setStyle(
                "-fx-font-size: 36px; -fx-font-weight: 900; -fx-text-fill: linear-gradient(to right, #667eea, #764ba2); "
                        + "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.5), 8, 0.6, 0, 2);");
        AnimationUtils.setupHoverEffect(logo);

        Label subtitle = new Label("Acceso de Operador");
        subtitle.setStyle(
                "-fx-text-fill: #6B7280; -fx-font-size: 15px; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.8), 1, 0, 0, 0.5);");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(14);
        user.setPromptText("👤 usuario");
        user.setStyle(
                "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                        + "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-border-radius: 12; "
                        + "-fx-background-color: white; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        pass.setPromptText("🔒 contraseña");
        pass.setStyle(
                "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                        + "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-border-radius: 12; "
                        + "-fx-background-color: white; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        passVisible.setPromptText("🔒 contraseña");
        passVisible.setStyle(
                "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                        + "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-border-radius: 12; "
                        + "-fx-background-color: white; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        // Vincular visibilidad de password
        passVisible.managedProperty().bind(showPass.selectedProperty());
        passVisible.visibleProperty().bind(showPass.selectedProperty());
        pass.managedProperty().bind(showPass.selectedProperty().not());
        pass.visibleProperty().bind(showPass.selectedProperty().not());
        passVisible.textProperty().bindBidirectional(pass.textProperty());

        Label userLabel = new Label("Usuario");
        userLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #374151;");
        Label passLabel = new Label("Contraseña");
        passLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #374151;");

        // Estilo del botón toggle para mostrar contraseña (estilo vibrante como el
        // botón de login)
        showPass.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600; " +
                        "-fx-padding: 8 16 8 16; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.3), 6, 0.4, 0, 2);");

        // Cambiar estilo cuando está seleccionado
        showPass.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                showPass.setText("🔒 Ocultar contraseña");
                showPass.setStyle(
                        "-fx-background-color: linear-gradient(to right, #764ba2, #667eea); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 13px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 8 16 8 16; " +
                                "-fx-background-radius: 8; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(118, 75, 162, 0.4), 8, 0.5, 0, 3);");
            } else {
                showPass.setText("👁 Mostrar contraseña");
                showPass.setStyle(
                        "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 13px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 8 16 8 16; " +
                                "-fx-background-radius: 8; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.3), 6, 0.4, 0, 2);");
            }
        });

        // Efecto hover en el botón
        showPass.setOnMouseEntered(e -> AnimationUtils.scaleIn(showPass, AnimationUtils.MICRO));
        showPass.setOnMouseExited(e -> {
            showPass.setScaleX(1.0);
            showPass.setScaleY(1.0);
        });

        form.addRow(0, userLabel, user);
        form.addRow(1, passLabel, pass);
        form.add(new Label(""), 0, 2);
        form.add(passVisible, 1, 1);
        form.add(showPass, 1, 2);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // El mensaje de estado debe verse completo: permitir varias líneas y ocupar el
        // ancho disponible
        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(status, Priority.ALWAYS);

        loginBtn.setDefaultButton(true);
        loginBtn.setMinWidth(180);
        loginBtn.setPrefWidth(180);
        loginBtn.setMaxWidth(180);
        loginBtn.setMinHeight(48);
        loginBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #667eea, #764ba2, #f093fb); "
                        + "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700; "
                        + "-fx-padding: 14 32 14 32; -fx-background-radius: 12; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.5), 12, 0.5, 0, 4);");
        // Colocar el status a la izquierda y el botón a la derecha; el status se
        // expande y hace wrap
        actions.getChildren().addAll(status, loginBtn);

        // Validación manual (no bind para evitar conflicto con showLoading)
        Runnable updateButton = () -> {
            boolean empty = user.getText().trim().isEmpty() || pass.getText().isEmpty();
            loginBtn.setDisable(empty);
        };
        user.textProperty().addListener((o, ov, nv) -> updateButton.run());
        pass.textProperty().addListener((o, ov, nv) -> updateButton.run());
        updateButton.run();

        user.setOnAction(e -> {
            if (!loginBtn.isDisable())
                loginBtn.fire();
        });
        pass.setOnAction(e -> {
            if (!loginBtn.isDisable())
                loginBtn.fire();
        });
        passVisible.setOnAction(e -> {
            if (!loginBtn.isDisable())
                loginBtn.fire();
        });

        loginBtn.setOnAction(e -> {
            clearStatus();
            AnimationUtils.pulse(loginBtn);
            handler.onLogin(user.getText().trim(), pass.getText());
        });

        // Efecto hover animado en botón
        loginBtn.setOnMouseEntered(e -> AnimationUtils.scaleIn(loginBtn, AnimationUtils.MICRO));
        loginBtn.setOnMouseExited(e -> {
            loginBtn.setScaleX(1.0);
            loginBtn.setScaleY(1.0);
        });

        card.getChildren().addAll(logo, subtitle, form, actions);

        StackPane.setAlignment(card, Pos.CENTER);
        getChildren().add(card);

        // Efectos de focus en inputs con color vibrante
        user.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (nowFocused) {
                user.setStyle(
                        "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                                + "-fx-border-color: #667eea; -fx-border-width: 2; -fx-border-radius: 12; "
                                + "-fx-background-color: white; "
                                + "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.3), 8, 0.5, 0, 2);");
            } else {
                user.setStyle(
                        "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                                + "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-border-radius: 12; "
                                + "-fx-background-color: white; "
                                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
            }
        });

        pass.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (nowFocused) {
                pass.setStyle(
                        "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                                + "-fx-border-color: #667eea; -fx-border-width: 2; -fx-border-radius: 12; "
                                + "-fx-background-color: white; "
                                + "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.3), 8, 0.5, 0, 2);");
            } else {
                pass.setStyle(
                        "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                                + "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-border-radius: 12; "
                                + "-fx-background-color: white; "
                                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
            }
        });

        passVisible.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (nowFocused) {
                passVisible.setStyle(
                        "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                                + "-fx-border-color: #667eea; -fx-border-width: 2; -fx-border-radius: 12; "
                                + "-fx-background-color: white; "
                                + "-fx-effect: dropshadow(gaussian, rgba(103, 126, 234, 0.3), 8, 0.5, 0, 2);");
            } else {
                passVisible.setStyle(
                        "-fx-font-size: 14px; -fx-padding: 12 16 12 16; -fx-background-radius: 12; "
                                + "-fx-border-color: #E5E7EB; -fx-border-width: 1.5; -fx-border-radius: 12; "
                                + "-fx-background-color: white; "
                                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
            }
        });

        // Animaciones de entrada combinadas con más movimiento
        AnimationUtils.fadeIn(card, AnimationUtils.MEDIUM);
        AnimationUtils.slideInLeft(card, AnimationUtils.MEDIUM);
        AnimationUtils.pulse(logo);
    }

    public void setStatus(String text) {
        status.setStyle("-fx-text-fill: #6B7280;");
        status.setText(text);
    }

    public void showLoading() {
        loginBtn.setDisable(true);
        loginBtn.setText("Iniciando...");
        setStatus("🔄 Autenticando credenciales…");
        AnimationUtils.pulse(card);
    }

    public void showError(String message) {
        status.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 700; -fx-font-size: 13px;");
        status.setText("❌ " + message);
        loginBtn.setText("Iniciar sesión");
        loginBtn.setDisable(false);
        AnimationUtils.shake(card);
        AnimationUtils.pulse(status);
    }

    public void clearStatus() {
        status.setText("");
        status.setStyle(null);
    }

    public Node getCard() {
        return card;
    }
}
