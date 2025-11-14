package co.cellano.edufeed.desktop.ui;

import co.cellano.edufeed.desktop.session.SessionContext;
import co.cellano.edufeed.desktop.theme.ThemeService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Barra superior con título, usuario actual, cambio de módulo y cierre de
 * sesión.
 */
public class NavBar extends HBox {
    private final Button switchBtn = new Button("Cambiar módulo");
    private final Button logoutBtn = new Button("Cerrar sesión");
    private final Label title = new Label();
    private final Label userLbl = new Label();
    private final ComboBox<ThemeService.Theme> themePicker = new ComboBox<>();

    public NavBar(String currentTitle, Runnable onChangeModule) {
        setSpacing(12);
        setPadding(new Insets(10, 12, 10, 12));
        getStyleClass().add("navbar");
        title.setText(currentTitle != null ? currentTitle : "");

        // Usuario actual
        String who = SessionContext.username != null && !SessionContext.username.isBlank()
                ? SessionContext.username
                : "Usuario";
        userLbl.setText("Usuario: " + who);
        userLbl.getStyleClass().add("navbar-user");

        // Espaciador flexible
        HBox spacer = new HBox();
        spacer.setMinWidth(10);
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Acciones
        switchBtn.getStyleClass().add("navbar-action");
        logoutBtn.getStyleClass().add("navbar-action");

        // Selector de tema
        themePicker.setItems(FXCollections.observableArrayList(ThemeService.Theme.values()));
        themePicker.getSelectionModel().select(ThemeService.getInstance().getCurrentTheme());
        themePicker.setPromptText("Tema");
        themePicker.setMinWidth(140);
        themePicker.setMaxWidth(180);
        themePicker.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ThemeService.Theme item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        themePicker.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ThemeService.Theme item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        getChildren().addAll(title, spacer, userLbl, themePicker, switchBtn, logoutBtn);

        switchBtn.setOnAction(e -> {
            if (onChangeModule != null)
                onChangeModule.run();
        });
        logoutBtn.setOnAction(e -> {
            if (SessionContext.onLogout != null)
                SessionContext.onLogout.run();
        });

        themePicker.setOnAction(e -> {
            ThemeService.Theme t = themePicker.getSelectionModel().getSelectedItem();
            if (t != null) {
                ThemeService.getInstance().setTheme(t, true);
            }
        });
    }

    public void setTitle(String t) {
        this.title.setText(t != null ? t : "");
    }
}
