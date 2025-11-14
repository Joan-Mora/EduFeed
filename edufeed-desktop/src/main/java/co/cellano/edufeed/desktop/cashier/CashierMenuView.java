package co.cellano.edufeed.desktop.cashier;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Vista principal del módulo de Caja con 3 opciones grandes.
 */
public class CashierMenuView extends VBox {

    private final Button historialBtn;
    private final Button asignarPagoBtn;
    private final Button cancelarFacturaBtn;

    public CashierMenuView() {
        getStyleClass().add("cashier-menu");
        setSpacing(24);
        setPadding(new Insets(32));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: -fx-background;");

        // Título principal
        Label titulo = new Label("Elige Una Opción");
        titulo.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: -fx-text;");

        // Contenedor de opciones - responsive con wrap
        HBox opcionesBox = new HBox(20);
        opcionesBox.setAlignment(Pos.CENTER);
        opcionesBox.setMaxWidth(1200);

        // Opción 1: Historial de Pagos
        VBox card1 = crearOpcionCard(
                "📊",
                "Historial de Pagos",
                "Consulta y gestiona todos los pagos realizados");
        historialBtn = new Button();
        historialBtn.setGraphic(card1);
        historialBtn.getStyleClass().addAll("app-button", "app-button--ghost", "menu-option-card");
        historialBtn.setMinWidth(280);
        historialBtn.setMaxWidth(380);
        historialBtn.setPrefWidth(340);
        historialBtn.setMinHeight(240);
        historialBtn.setMaxHeight(240);
        historialBtn.setPrefHeight(240);

        // Opción 2: Asignar Pago
        VBox card2 = crearOpcionCard(
                "💰",
                "Asignar Pago",
                "Registra un nuevo pago para un usuario");
        asignarPagoBtn = new Button();
        asignarPagoBtn.setGraphic(card2);
        asignarPagoBtn.getStyleClass().addAll("app-button", "app-button--ghost", "menu-option-card");
        asignarPagoBtn.setMinWidth(280);
        asignarPagoBtn.setMaxWidth(380);
        asignarPagoBtn.setPrefWidth(340);
        asignarPagoBtn.setMinHeight(240);
        asignarPagoBtn.setMaxHeight(240);
        asignarPagoBtn.setPrefHeight(240);

        // Opción 3: Cancelar Factura
        VBox card3 = crearOpcionCard(
                "❌",
                "Cancelar Factura",
                "Anula una factura existente");
        cancelarFacturaBtn = new Button();
        cancelarFacturaBtn.setGraphic(card3);
        cancelarFacturaBtn.getStyleClass().addAll("app-button", "app-button--ghost", "menu-option-card");
        cancelarFacturaBtn.setMinWidth(280);
        cancelarFacturaBtn.setMaxWidth(380);
        cancelarFacturaBtn.setPrefWidth(340);
        cancelarFacturaBtn.setMinHeight(240);
        cancelarFacturaBtn.setMaxHeight(240);
        cancelarFacturaBtn.setPrefHeight(240);

        // No usar grow para mantener tamaños uniformes
        opcionesBox.getChildren().addAll(historialBtn, asignarPagoBtn, cancelarFacturaBtn);

        getChildren().addAll(titulo, opcionesBox);

        // Estilo de las cards
        String cardStyle = """
                -fx-background-color: -fx-surface;
                -fx-background-radius: 12px;
                -fx-border-color: -fx-border;
                -fx-border-radius: 12px;
                -fx-border-width: 1px;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);
                """;

        historialBtn.setStyle(cardStyle);
        asignarPagoBtn.setStyle(cardStyle);
        cancelarFacturaBtn.setStyle(cardStyle);

        // Efectos hover
        agregarEfectoHover(historialBtn);
        agregarEfectoHover(asignarPagoBtn);
        agregarEfectoHover(cancelarFacturaBtn);
    }

    private VBox crearOpcionCard(String icono, String titulo, String descripcion) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMinHeight(220);
        card.setMaxHeight(220);
        card.setPrefHeight(220);
        card.setMinWidth(260);
        card.setMaxWidth(360);
        card.setPrefWidth(320);

        Label iconoLabel = new Label(icono);
        iconoLabel.setStyle("-fx-font-size: 56px;");

        Label tituloLabel = new Label(titulo);
        tituloLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -fx-text; -fx-text-alignment: center;");
        tituloLabel.setWrapText(true);
        tituloLabel.setMaxWidth(300);
        tituloLabel.setAlignment(Pos.CENTER);

        Label descLabel = new Label(descripcion);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-text-secondary; -fx-text-alignment: center;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(300);
        descLabel.setMinHeight(36); // Espacio fijo para 2-3 líneas
        descLabel.setAlignment(Pos.TOP_CENTER);

        card.getChildren().addAll(iconoLabel, tituloLabel, descLabel);
        return card;
    }

    private void agregarEfectoHover(Button btn) {
        btn.setOnMouseEntered(e -> {
            btn.setStyle(btn.getStyle() +
                    "-fx-scale-x: 1.05; -fx-scale-y: 1.05; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(btn.getStyle().replaceAll("-fx-scale-[xy]: [0-9.]+;", ""));
        });
    }

    public Button getHistorialBtn() {
        return historialBtn;
    }

    public Button getAsignarPagoBtn() {
        return asignarPagoBtn;
    }

    public Button getCancelarFacturaBtn() {
        return cancelarFacturaBtn;
    }
}
