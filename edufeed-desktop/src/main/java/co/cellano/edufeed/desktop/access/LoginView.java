package co.cellano.edufeed.desktop.access;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class LoginView extends BorderPane {
    public interface LoginHandler { void onLogin(String username, String password); }

    private final TextField user = new TextField();
    private final PasswordField pass = new PasswordField();
    private final Button loginBtn = new Button("Iniciar sesión");
    private final Label status = new Label();

    public LoginView(LoginHandler handler) {
        setPadding(new Insets(16));
        var title = new Label("EduFeed — Acceso Operador");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        setTop(title); BorderPane.setAlignment(title, Pos.CENTER);

        var form = new GridPane();
        form.setHgap(8); form.setVgap(10); form.setPadding(new Insets(12));
        user.setPromptText("usuario");
        pass.setPromptText("contraseña");
        form.addRow(0, new Label("Usuario:"), user);
        form.addRow(1, new Label("Contraseña:"), pass);

        var actions = new HBox(10, loginBtn, status);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8));

        setCenter(form);
        setBottom(actions);

        loginBtn.setOnAction(e -> {
            status.setText("");
            handler.onLogin(user.getText().trim(), pass.getText());
        });
    }

    public void setStatus(String text) { status.setText(text); }
}
