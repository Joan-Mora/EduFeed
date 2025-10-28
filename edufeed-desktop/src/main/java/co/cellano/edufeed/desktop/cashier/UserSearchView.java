package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Vista de búsqueda de usuarios: por documento o por nombre (parcial, con debounce). */
public class UserSearchView extends VBox {
    private final TextField tfDocumento = new TextField();
    private final TextField tfNombre = new TextField();
    private final Button btnBuscarDoc = new Button("Buscar");
    private final Label lbLatency = new Label();
    private final TableView<UsuarioDto> table = new TableView<>();
    private final ProgressIndicator progress = new ProgressIndicator();

    private Consumer<String> onSearchDocumento;
    private Consumer<String> onSearchNombre;
    private Consumer<UsuarioDto> onSelect;

    private final Timer debounceTimer = new Timer(true);
    private TimerTask pendingTask;

    public UserSearchView() {
        setSpacing(8);
        setPadding(new Insets(12));

        tfDocumento.setPromptText("Documento (exacto)");
        tfNombre.setPromptText("Nombre (parcial)");
        progress.setVisible(false);
        progress.setPrefSize(20,20);

        HBox line1 = new HBox(6, new Label("Documento:"), tfDocumento, btnBuscarDoc, new Label("Latencia:"), lbLatency, progress);
        HBox.setHgrow(tfDocumento, Priority.ALWAYS);

        HBox line2 = new HBox(6, new Label("Nombre:"), tfNombre);
        HBox.setHgrow(tfNombre, Priority.ALWAYS);

        TableColumn<UsuarioDto, String> cNombre = new TableColumn<>("Nombre");
        cNombre.setCellValueFactory(d -> new ReadOnlyStringWrapperSafe(d.getValue().nombreCompleto));
        cNombre.setPrefWidth(260);
        TableColumn<UsuarioDto, String> cDoc = new TableColumn<>("Documento");
        cDoc.setCellValueFactory(d -> new ReadOnlyStringWrapperSafe(d.getValue().documento));
        cDoc.setPrefWidth(160);
        table.getColumns().addAll(cNombre, cDoc);
        table.setPlaceholder(new Label("Sin resultados"));

        getChildren().addAll(new Label("Búsqueda de usuario"), line1, line2, table);

        btnBuscarDoc.setOnAction(e -> {
            if (onSearchDocumento != null) onSearchDocumento.accept(tfDocumento.getText().trim());
        });

        tfNombre.textProperty().addListener((obs, o, n) -> {
            if (Objects.equals(o,n)) return;
            if (pendingTask != null) pendingTask.cancel();
            String q = n == null ? "" : n.trim();
            if (q.length() < 2) return; // evita búsquedas triviales
            pendingTask = new TimerTask() {
                @Override public void run() { Platform.runLater(() -> {
                    if (onSearchNombre != null) onSearchNombre.accept(q);
                }); }
            };
            debounceTimer.schedule(pendingTask, 250); // debounce 250ms
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && onSelect != null) onSelect.accept(n);
        });
    }

    public void setOnSearchDocumento(Consumer<String> c) { this.onSearchDocumento = c; }
    public void setOnSearchNombre(Consumer<String> c) { this.onSearchNombre = c; }
    public void setOnSelect(Consumer<UsuarioDto> c) { this.onSelect = c; }

    public void setResults(List<UsuarioDto> list, long latencyMs) {
        table.getItems().setAll(list);
        lbLatency.setText(latencyMs + " ms");
    }

    public void setBusy(boolean busy) { progress.setVisible(busy); }

    /** Devuelve el usuario actualmente seleccionado en la tabla (o null). */
    public UsuarioDto getSelectedUser() {
        return table.getSelectionModel().getSelectedItem();
    }

    // Safe wrapper because javafx.beans.property.ReadOnlyStringWrapper isn't imported;
    private static class ReadOnlyStringWrapperSafe extends javafx.beans.property.ReadOnlyStringWrapper {
        public ReadOnlyStringWrapperSafe(String value) { super(value==null?"":value); }
    }
}
