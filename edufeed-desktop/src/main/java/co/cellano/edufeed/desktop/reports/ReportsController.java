package co.cellano.edufeed.desktop.reports;

import co.cellano.edufeed.desktop.service.ReportApiClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ReportsController {
    private final Stage stage;

    private final ReportsView view = new ReportsView();
    private final ReportApiClient api;

    // Estado actual
    private List<?> currentData = new ArrayList<>();
    private int pageSize = 25;

    public ReportsController(Stage stage, String baseUrl, String token) {
        this.stage = stage;
        this.api = new ReportApiClient(baseUrl, token);
    }

    public void start() {
        BorderPane root = view.getRoot();
        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("EduFeed — Reportes");
        stage.setScene(scene);
        // Maximizar razonablemente
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        stage.setX(vb.getMinX()); stage.setY(vb.getMinY());
        stage.setWidth(Math.min(1280, vb.getWidth()));
        stage.setHeight(Math.min(800, vb.getHeight()));
        stage.show();

        wireEvents();
        refreshUiForType();
    }

    private void wireEvents() {
        view.reportType.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> refreshUiForType());
        view.buscar.setOnAction(e -> runSearch());
        view.exportCsv.setOnAction(e -> runExportCsv());
    }

    private void refreshUiForType() {
        String type = getTypeKey(view.reportType.getValue());
        boolean needsRange = !"derechos-activos".equals(type);
        view.desde.setDisable(!needsRange);
        view.hasta.setDisable(!needsRange);
        buildTableColumns(type);
        view.resumen.setText("");
        view.table.setItems(FXCollections.observableArrayList());
        view.pagination.setPageCount(1);
        view.pagination.setCurrentPageIndex(0);
    }

    private String getTypeKey(String choice) {
        if (choice == null) return "ingresos";
        return switch (choice) {
            case "Ingresos" -> "ingresos";
            case "Asistencias" -> "asistencias";
            case "Rechazos" -> "rechazos";
            case "Derechos activos" -> "derechos-activos";
            default -> "ingresos";
        };
    }

    private void buildTableColumns(String type) {
        TableView<Object> table = view.table;
        table.getColumns().clear();
        switch (type) {
            case "ingresos" -> {
                table.getColumns().add(col("Día", "dia"));
                table.getColumns().add(col("Tipo pago", "tipoPago"));
                table.getColumns().add(col("Método", "metodoPago"));
                table.getColumns().add(col("Cantidad", "cantidad"));
                table.getColumns().add(col("Total", "total"));
            }
            case "asistencias" -> {
                table.getColumns().add(col("Día", "dia"));
                table.getColumns().add(col("Accesos", "totalAccesos"));
                table.getColumns().add(col("Usuarios únicos", "usuariosUnicos"));
            }
            case "rechazos" -> {
                table.getColumns().add(col("Día", "dia"));
                table.getColumns().add(col("Motivo", "motivoRechazo"));
                table.getColumns().add(col("Cantidad", "cantidad"));
            }
            case "derechos-activos" -> {
                table.getColumns().add(col("Documento", "usuarioDocumento"));
                table.getColumns().add(col("Nombre", "usuarioNombre"));
                table.getColumns().add(col("Derecho", "tipoDerecho"));
                table.getColumns().add(col("Desde", "vigenteDesde"));
                table.getColumns().add(col("Hasta", "vigenteHasta"));
                table.getColumns().add(col("Días restantes", "diasRestantes"));
            }
        }
    }

    private <T> TableColumn<Object, T> col(String title, String prop) {
        TableColumn<Object, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(150);
        return c;
    }

    private void runSearch() {
        String type = getTypeKey(view.reportType.getValue());
        LocalDate d = view.desde.getValue();
        LocalDate h = view.hasta.getValue();
        view.buscar.setDisable(true);
        view.exportCsv.setDisable(true);
        view.resumen.setText("Consultando…");

        new Thread(() -> {
            try {
                switch (type) {
                    case "ingresos" -> {
                        List<ReportApiClient.IngresosDiariosItem> data = api.ingresos(d, h);
                        BigDecimal sum = api.resumenIngresos(d, h);
                        Platform.runLater(() -> {
                            applyData(data);
                            view.resumen.setText("Total ingresos: $" + sum);
                        });
                    }
                    case "asistencias" -> {
                        var data = api.asistencias(d, h);
                        Platform.runLater(() -> {
                            applyData(data);
                            view.resumen.setText("Registros: " + data.size());
                        });
                    }
                    case "rechazos" -> {
                        var data = api.rechazos(d, h);
                        Platform.runLater(() -> {
                            applyData(data);
                            view.resumen.setText("Registros: " + data.size());
                        });
                    }
                    case "derechos-activos" -> {
                        var data = api.derechosActivos();
                        Platform.runLater(() -> {
                            applyData(data);
                            view.resumen.setText("Registros: " + data.size());
                        });
                    }
                }
            } catch (IOException ex) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error al consultar: " + ex.getMessage(), ButtonType.OK).showAndWait());
            } finally {
                Platform.runLater(() -> { view.buscar.setDisable(false); view.exportCsv.setDisable(false); });
            }
        }).start();
    }

    private void applyData(List<?> data) {
        this.currentData = data;
        paginate(0);
        int totalPages = Math.max(1, (int) Math.ceil((double) currentData.size() / pageSize));
        view.pagination.setPageCount(totalPages);
        view.pagination.currentPageIndexProperty().addListener((obs, oldV, newV) -> paginate(newV.intValue()));
    }

    private void paginate(int pageIndex) {
        int from = Math.max(0, pageIndex * pageSize);
        int to = Math.min(from + pageSize, currentData.size());
        var sub = currentData.subList(from, to);
        // Conversión a lista de Object para salvar comodines
        @SuppressWarnings("unchecked")
        var slice = FXCollections.observableArrayList((java.util.List<Object>)(java.util.List<?>) sub);
        // El PropertyValueFactory usa reflexión de getters/campos según columnas definidas
        view.table.setItems(slice);
    }

    private void runExportCsv() {
        String type = getTypeKey(view.reportType.getValue());
        LocalDate d = view.desde.getValue();
        LocalDate h = view.hasta.getValue();

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName(suggestFileName(type));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;

        view.exportCsv.setDisable(true);
        view.resumen.setText("Exportando…");
        new Thread(() -> {
            try {
                byte[] bytes = api.exportCsv(type, d, h);
                try (FileOutputStream os = new FileOutputStream(f)) { os.write(bytes); }
                Platform.runLater(() -> view.resumen.setText("Exportado: " + f.getName()));
            } catch (IOException ex) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error al exportar: " + ex.getMessage(), ButtonType.OK).showAndWait());
            } finally {
                Platform.runLater(() -> view.exportCsv.setDisable(false));
            }
        }).start();
    }

    private String suggestFileName(String type) {
        String base = switch (type) {
            case "ingresos" -> "ingresos";
            case "asistencias" -> "asistencias";
            case "rechazos" -> "rechazos";
            case "derechos-activos" -> "derechos_activos";
            default -> type;
        };
        return base + ".csv";
    }
}
