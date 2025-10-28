package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Vista principal de caja: búsqueda + formulario + estado. */
public class CashierView extends BorderPane {
    public final UserSearchView userSearch = new UserSearchView();
    public final PaymentFormView paymentForm = new PaymentFormView();

    private final Label lbSelectedUser = new Label("Sin usuario seleccionado");
    private final Label lbStatus = new Label();

    public CashierView() {
        setPadding(new Insets(10));
        setTop(buildTop());
        setCenter(buildCenter());
        setBottom(buildBottom());
    }

    private Node buildTop() {
        VBox box = new VBox(6, new Label("Módulo de Caja"), lbSelectedUser);
        box.setPadding(new Insets(6));
        return box;
    }

    private Node buildCenter() {
        SplitPane split = new SplitPane();
        split.getItems().addAll(wrap("Búsqueda", userSearch), wrap("Pago", paymentForm));
        split.setDividerPositions(0.5);
        return split;
    }

    private Node buildBottom() {
        HBox box = new HBox(10, new Label("Estado:"), lbStatus);
        box.setPadding(new Insets(6));
        return box;
    }

    public void setSelectedUser(UsuarioDto u) {
        lbSelectedUser.setText(u == null ? "Sin usuario seleccionado" : "Usuario: " + u.nombreCompleto + " (" + u.documento + ")");
    }

    public void setStatus(String msg) { lbStatus.setText(msg==null?"":msg); }

    private TitledPane wrap(String title, Node content) {
        TitledPane tp = new TitledPane(title, content);
        tp.setCollapsible(false);
        return tp;
    }
}
