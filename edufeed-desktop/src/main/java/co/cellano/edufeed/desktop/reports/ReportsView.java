package co.cellano.edufeed.desktop.reports;

import java.time.LocalDate;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/** Vista principal para el módulo de Reportes */
public class ReportsView {
    public final ChoiceBox<String> reportType = new ChoiceBox<>();
    public final DatePicker desde = new DatePicker();
    public final DatePicker hasta = new DatePicker();
    public final Button buscar = new Button("Buscar");
    public final Button exportCsv = new Button("Exportar CSV");
    public final Label resumen = new Label("");
    public final TableView<Object> table = new TableView<>();
    public final Pagination pagination = new Pagination();

    private final BorderPane root = new BorderPane();

    public ReportsView() {
        reportType.getItems().addAll("Ingresos", "Asistencias", "Rechazos", "Derechos activos");
        reportType.getSelectionModel().selectFirst();

        GridPane filtros = new GridPane();
        filtros.setHgap(10); filtros.setVgap(8); filtros.setPadding(new Insets(10));
        filtros.add(new Label("Reporte:"), 0, 0);
        filtros.add(reportType, 1, 0);
        filtros.add(new Label("Desde:"), 2, 0);
        filtros.add(desde, 3, 0);
        filtros.add(new Label("Hasta:"), 4, 0);
        filtros.add(hasta, 5, 0);

        HBox acciones = new HBox(10, buscar, exportCsv, resumen);
        acciones.setPadding(new Insets(10, 10, 10, 10));

        BorderPane top = new BorderPane();
        top.setLeft(filtros);
        top.setBottom(acciones);

        pagination.setPageFactory(pageIndex -> (Node) table);
        pagination.setMaxPageIndicatorCount(10);

        root.setTop(top);
        root.setCenter(table);
        root.setBottom(pagination);

        // Por defecto, rango de últimos 7 días para los que aplican
        LocalDate hoy = LocalDate.now();
        desde.setValue(hoy.minusDays(7));
        hasta.setValue(hoy);
    }

    public BorderPane getRoot() { return root; }
}
