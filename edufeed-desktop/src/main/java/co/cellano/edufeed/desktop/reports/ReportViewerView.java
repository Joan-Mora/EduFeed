package co.cellano.edufeed.desktop.reports;

import javafx.scene.Node;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/** Contenedor reutilizable para tabla + paginación (simple) */
public class ReportViewerView {
    public final TableView<?> table = new TableView<>();
    public final Pagination pagination = new Pagination();

    private final BorderPane root = new BorderPane();

    public ReportViewerView() {
        pagination.setPageFactory((Integer pageIndex) -> (Node) table);
        root.setCenter(table);
        root.setBottom(pagination);
    }

    public BorderPane getRoot() { return root; }
}
