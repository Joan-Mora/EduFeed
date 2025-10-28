package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.cashier.PaymentFormView.SubmitData;
import co.cellano.edufeed.desktop.service.PaymentApiClient;
import co.cellano.edufeed.desktop.service.PaymentApiClient.PagoDto;
import co.cellano.edufeed.desktop.service.PaymentApiClient.TipoPago;
import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Controlador que orquesta búsqueda y creación de pagos. */
public class CashierController {
    private final Stage stage;
    private final String baseUrl;
    private final String bearer;
    private final CashierView view = new CashierView();
    private PaymentApiClient api;

    public CashierController(Stage stage, String baseUrl, String bearer) {
        this.stage = stage; this.baseUrl = baseUrl; this.bearer = bearer;
        this.api = new PaymentApiClient(baseUrl, bearer);
        wire();
    }

    public void start() {
        stage.setScene(new Scene(view, 920, 560));
        stage.setTitle("EduFeed — Caja");
        stage.show();
    }

    private void wire() {
        view.userSearch.setOnSearchDocumento(doc -> runAsync(() -> doBuscarPorDocumento(doc)));
        view.userSearch.setOnSearchNombre(q -> runAsync(() -> doBuscarPorNombre(q)));
        view.userSearch.setOnSelect(u -> view.setSelectedUser(u));

        view.paymentForm.setOnSubmit((data, onDone) -> runAsync(() -> doCrearPago(data, onDone)));
    }

    private void doBuscarPorDocumento(String doc) {
        try {
            long t0 = System.nanoTime();
            var opt = api.buscarUsuarioPorDocumento(doc);
            long dt = (System.nanoTime() - t0) / 1_000_000; // ms
            List<UsuarioDto> results = opt.map(Collections::singletonList).orElse(Collections.emptyList());
            Platform.runLater(() -> {
                view.userSearch.setResults(results, dt);
                if (!results.isEmpty()) view.setSelectedUser(results.get(0));
            });
        } catch (IOException e) {
            Platform.runLater(() -> view.setStatus("Error búsqueda documento: "+e.getMessage()));
        }
    }

    private void doBuscarPorNombre(String q) {
        try {
            long t0 = System.nanoTime();
            List<UsuarioDto> results = api.buscarUsuariosPorNombre(q);
            long dt = (System.nanoTime() - t0) / 1_000_000; // ms
            Platform.runLater(() -> view.userSearch.setResults(results, dt));
        } catch (IOException e) {
            Platform.runLater(() -> view.setStatus("Error búsqueda nombre: "+e.getMessage()));
        }
    }

    private void doCrearPago(SubmitData data, Runnable onDone) {
        try {
            UsuarioDto u = getSelectedUserOrFail();
            PaymentApiClient.CreatePagoRequest req = new PaymentApiClient.CreatePagoRequest(
                    u.id,
                    data.monto(),
                    data.tipo(),
                    data.metodo(),
                    data.referencia(),
                    data.tipo() == TipoPago.PAQUETE ? data.diasPaquete() : null,
                    System.getProperty("user.name", "cajero"),
                    null
            );
            PagoDto creado = api.crearPago(req);
            if (data.aprobarAuto() && creado != null && creado.id != null && !creado.id.isBlank()) {
                try { creado = api.aprobarPago(UUID.fromString(creado.id)); }
                catch (Exception ex) { /* si falla aprobación, mostrar pero no bloquear */
                    final String msg = ex.getMessage();
                    Platform.runLater(() -> view.setStatus("Pago creado pero falla aprobar: "+msg));
                }
            }
            final PagoDto pagoFinal = creado;
            Platform.runLater(() -> view.setStatus("Pago "+ (pagoFinal!=null?pagoFinal.estadoPago:"CREADO") +" ID="+ (pagoFinal!=null?pagoFinal.id:"?")));
        } catch (Exception e) {
            Platform.runLater(() -> view.setStatus("Error creando pago: "+e.getMessage()));
        } finally {
            Platform.runLater(onDone);
        }
    }

    private UsuarioDto getSelectedUserOrFail() {
        UsuarioDto selected = view.userSearch.getSelectedUser();
        if (selected == null) throw new IllegalStateException("Seleccione un usuario");
        return selected;
    }

    private void runAsync(Runnable r) { new Thread(r, "cashier").start(); }
}
