package co.cellano.edufeed.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class DesktopApp extends Application {
    private final OkHttpClient http = new OkHttpClient();

    @Override
    public void start(Stage stage) {
        var baseUrl = System.getenv().getOrDefault("BACKEND_BASE_URL", "http://localhost:8080");
        var label = new Label("EduFeed Desktop — Backend: " + baseUrl);
        var btn = new Button("Probar /health");
        btn.setOnAction(ev -> {
            try {
                var req = new Request.Builder().url(baseUrl + "/health").build();
                try (var res = http.newCall(req).execute()) {
                    label.setText("/health => " + res.code() + ": " + res.body().string());
                }
            } catch (Exception e) {
                label.setText("Error: " + e.getMessage());
            }
        });
        var root = new VBox(10, label, btn);
        stage.setScene(new Scene(root, 520, 180));
        stage.setTitle("EduFeed Desktop");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
