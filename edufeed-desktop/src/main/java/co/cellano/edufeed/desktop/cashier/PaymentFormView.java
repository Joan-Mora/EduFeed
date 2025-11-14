package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient.TipoPago;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Formulario con validación en tiempo real y cálculo de monto por tipo. */
public class PaymentFormView extends VBox {
    // Inputs
    private final ComboBox<TipoPago> cbTipo = new ComboBox<>();
    private final Spinner<Integer> spDias = new Spinner<>(1, 90, 5);
    private final TextField tfMonto = new TextField();
    private final ComboBox<String> cbMetodo = new ComboBox<>();
    private final TextField tfReferencia = new TextField();
    private final CheckBox cbAprobarAuto = new CheckBox("Aprobar automáticamente");
    private final Button btnConfirmar = new Button("Confirmar pago");
    private final Label lbError = new Label();

    // Estado
    private final ObjectProperty<TipoPago> tipoPago = new SimpleObjectProperty<>(TipoPago.DIARIO);
    private final IntegerProperty diasPaquete = new SimpleIntegerProperty(5);
    private final ObjectProperty<BigDecimal> monto = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // Tarifas (pueden venir de env o config), valores por defecto
    private final BigDecimal tarifaDiario;
    private final BigDecimal tarifaMensual;
    private final BigDecimal tarifaPaqueteDia;

    private BiConsumer<SubmitData, Runnable> onSubmit; // (datos, onDone)

    public PaymentFormView() {
        this(null, null, null);
    }

    public PaymentFormView(BigDecimal tarifaDiario, BigDecimal tarifaMensual, BigDecimal tarifaPaqueteDia) {
        setSpacing(8);
        setPadding(new Insets(12));
        this.tarifaDiario = tarifaDiario != null ? tarifaDiario
                : safeBD(System.getenv().getOrDefault("DESKTOP_TARIFA_DIARIO", "5000"));
        this.tarifaMensual = tarifaMensual != null ? tarifaMensual
                : safeBD(System.getenv().getOrDefault("DESKTOP_TARIFA_MENSUAL", "60000"));
        this.tarifaPaqueteDia = tarifaPaqueteDia != null ? tarifaPaqueteDia
                : safeBD(System.getenv().getOrDefault("DESKTOP_TARIFA_PAQUETE_DIA", "12000"));

        getChildren().add(new Label("Formulario de pago"));

        cbTipo.getItems().addAll(TipoPago.DIARIO, TipoPago.MENSUAL, TipoPago.PAQUETE);
        cbTipo.getSelectionModel().select(TipoPago.DIARIO);
        cbMetodo.getItems().addAll("EFECTIVO", "TARJETA", "TRANSFERENCIA", "POS");
        cbMetodo.getSelectionModel().select("EFECTIVO");
        tfMonto.setEditable(false);
        spDias.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int r = 0;
        grid.add(new Label("Tipo:"), 0, r);
        grid.add(cbTipo, 1, r++);
        grid.add(new Label("Días (paquete):"), 0, r);
        grid.add(spDias, 1, r++);
        grid.add(new Label("Monto:"), 0, r);
        grid.add(tfMonto, 1, r++);
        grid.add(new Label("Método pago:"), 0, r);
        grid.add(cbMetodo, 1, r++);
        grid.add(new Label("Referencia:"), 0, r);
        grid.add(tfReferencia, 1, r++);
        grid.add(cbAprobarAuto, 1, r++);
        grid.add(btnConfirmar, 1, r++);
        lbError.getStyleClass().add("error-text");
        grid.add(lbError, 1, r++);

        getChildren().add(grid);

        // Binding estado
        tipoPago.bind(cbTipo.getSelectionModel().selectedItemProperty());
        diasPaquete.bind(spDias.valueProperty());

        // Mostrar/ocultar días
        spDias.disableProperty()
                .bind(Bindings.createBooleanBinding(() -> tipoPago.get() != TipoPago.PAQUETE, tipoPago));

        // Cálculo de monto en tiempo real
        Runnable recalc = () -> {
            try {
                TipoPago t = tipoPago.get();
                BigDecimal m;
                if (t == TipoPago.DIARIO)
                    m = nz(tarifaDiario);
                else if (t == TipoPago.MENSUAL)
                    m = nz(tarifaMensual);
                else {
                    BigDecimal base = nz(tarifaPaqueteDia);
                    int dias = Math.max(1, diasPaquete.get());
                    m = base.multiply(BigDecimal.valueOf(dias));
                }
                if (m == null)
                    m = BigDecimal.ZERO;
                monto.set(m);
                tfMonto.setText(m.toPlainString());
            } catch (Exception ex) {
                monto.set(BigDecimal.ZERO);
                tfMonto.setText("0");
            }
        };
        cbTipo.valueProperty().addListener((o, a, b) -> recalc.run());
        spDias.valueProperty().addListener((o, a, b) -> recalc.run());
        recalc.run();

        btnConfirmar.setOnAction(e -> submit());
    }

    private void submit() {
        lbError.setText("");
        if (monto.get() == null || monto.get().compareTo(BigDecimal.ZERO) <= 0) {
            lbError.setText("Monto inválido");
            return;
        }
        if (cbMetodo.getValue() == null || cbMetodo.getValue().isBlank()) {
            lbError.setText("Seleccione método de pago");
            return;
        }
        SubmitData data = new SubmitData(tipoPago.get(), diasPaquete.get(), monto.get(), cbMetodo.getValue(),
                tfReferencia.getText().trim(), cbAprobarAuto.isSelected());
        if (onSubmit != null)
            onSubmit.accept(data, () -> btnConfirmar.setDisable(false));
        btnConfirmar.setDisable(true);
    }

    public void setOnSubmit(BiConsumer<SubmitData, Runnable> onSubmit) {
        this.onSubmit = onSubmit;
    }

    public record SubmitData(TipoPago tipo, int diasPaquete, BigDecimal monto, String metodo, String referencia,
            boolean aprobarAuto) {
    }

    private static BigDecimal safeBD(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
