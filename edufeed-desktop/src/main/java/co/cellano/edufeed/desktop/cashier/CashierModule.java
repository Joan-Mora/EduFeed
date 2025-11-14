package co.cellano.edufeed.desktop.cashier;

import co.cellano.edufeed.desktop.service.PaymentApiClient;
import co.cellano.edufeed.desktop.service.PaymentApiClient.PagoDto;
import co.cellano.edufeed.desktop.service.PaymentApiClient.PagoEnriquecidoDto;
import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import co.cellano.edufeed.desktop.util.AnimationUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Módulo de Caja embebible en MainShell con menú de opciones.
 */
public class CashierModule {

    private final StackPane container = new StackPane();
    private final PaymentApiClient api;

    // Vistas
    private CashierMenuView menuView;
    private PaymentHistoryView historyView;
    private AssignPaymentView assignView;
    private CancelInvoiceView cancelView;

    public CashierModule(String baseUrl, String token) {
        this.api = new PaymentApiClient(baseUrl, token);
        initializeViews();
        showMenu();
    }

    /**
     * Retorna la vista principal del módulo (embebible)
     */
    public StackPane getView() {
        AnimationUtils.fadeIn(container);
        return container;
    }

    private void initializeViews() {
        // Inicializar menú
        menuView = new CashierMenuView();
        menuView.getHistorialBtn().setOnAction(e -> showHistory());
        menuView.getAsignarPagoBtn().setOnAction(e -> showAssignPayment());
        menuView.getCancelarFacturaBtn().setOnAction(e -> showCancelInvoice());

        // Inicializar historial con nueva vista
        historyView = new PaymentHistoryView();
        wireHistoryView();

        // Inicializar vista de asignación
        assignView = new AssignPaymentView();
        wireAssignView();

        // Inicializar vista de cancelación
        cancelView = new CancelInvoiceView();
        wireCancelView();
    }

    private void wireHistoryView() {
        // Regresar al menú
        historyView.setOnRegresar(() -> showMenu());

        // Buscar pagos (carga inicial)
        historyView.setOnBuscar(() -> {
            runAsync(() -> cargarHistorialPagos());
        });

        // Revertir pago aprobado
        historyView.setOnRevertir(pago -> {
            Platform.runLater(() -> mostrarDialogoDevolucion(pago));
        });

        // Editar motivo de devolución
        historyView.setOnEditarMotivoDevolucion(pago -> {
            Platform.runLater(() -> editarMotivoDevolucion(pago));
        });
    }

    private void cargarHistorialPagos() {
        try {
            List<PagoEnriquecidoDto> pagos = api.listarPagos();
            Platform.runLater(() -> historyView.setResultados(pagos));
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error al cargar historial");
                alert.setContentText("No se pudo cargar el historial de pagos: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void mostrarDialogoDevolucion(PagoEnriquecidoDto pago) {
        // Crear diálogo personalizado con detalles del pago y campo para motivo
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Revertir Pago - Devolución");
        dialog.setHeaderText("Detalle del Pago a Revertir");

        // Botones
        ButtonType revertirButtonType = new ButtonType("🔄 Revertir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(revertirButtonType, ButtonType.CANCEL);

        // Crear contenido del diálogo
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f8f9fa;");

        // Información del pago
        Label infoLabel = new Label("📄 Información del Pago");
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        grid.add(new Label("Referencia:"), 0, 0);
        grid.add(new Label(pago.referenciaExterna != null ? pago.referenciaExterna : "N/A"), 1, 0);

        grid.add(new Label("Usuario:"), 0, 1);
        grid.add(new Label(pago.usuarioNombre != null ? pago.usuarioNombre : "N/A"), 1, 1);

        grid.add(new Label("Documento:"), 0, 2);
        grid.add(new Label(pago.usuarioDocumento != null ? pago.usuarioDocumento : "N/A"), 1, 2);

        grid.add(new Label("Monto:"), 0, 3);
        Label montoLabel = new Label(pago.monto != null ? "$" + String.format("%,.0f", pago.monto) : "$0");
        montoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745; -fx-font-size: 14px;");
        grid.add(montoLabel, 1, 3);

        grid.add(new Label("Tipo:"), 0, 4);
        grid.add(new Label(pago.tipoPago != null ? pago.tipoPago.name() : "N/A"), 1, 4);

        // Campo para motivo de devolución
        Label motivoLabel = new Label("🔄 Motivo de Devolución:");
        motivoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea motivoArea = new TextArea();
        motivoArea.setPromptText("Escriba el motivo por el cual se revierte este pago...");
        motivoArea.setPrefRowCount(4);
        motivoArea.setWrapText(true);
        motivoArea.setStyle("-fx-font-size: 13px;");

        content.getChildren().addAll(infoLabel, grid, new Separator(), motivoLabel, motivoArea);
        dialog.getDialogPane().setContent(content);

        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == revertirButtonType) {
                return motivoArea.getText();
            }
            return null;
        });

        // Mostrar diálogo y procesar resultado
        dialog.showAndWait().ifPresent(motivo -> {
            runAsync(() -> ejecutarDevolucion(pago, motivo));
        });
    }

    private void ejecutarDevolucion(PagoEnriquecidoDto pago, String motivoDevolucion) {
        try {
            // 1. Revertir el pago (cambia estado a RECHAZADO en backend)
            api.revertirPago(UUID.fromString(pago.id));

            // 2. Actualizar metadatos con estado REVERTIDO y motivo de devolución
            String metadatosActual = pago.metadatos != null ? pago.metadatos : "{}";
            String nuevoJson = mergeMotivoDevolución(metadatosActual, motivoDevolucion);
            api.actualizarMetadatosPago(UUID.fromString(pago.id), nuevoJson);

            // 3. Actualizar estado del pago a REVERTIDO mediante metadatos
            // (Como el backend no tiene estado REVERTIDO, lo manejamos en el cliente)

            Platform.runLater(() -> {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Pago Revertido");
                success.setHeaderText("✓ Devolución procesada exitosamente");
                success.setContentText(String.format(
                        "El pago ha sido revertido:\n\nReferencia: %s\nUsuario: %s\nMonto: $%,.0f\n\nMotivo: %s\n\nEl saldo será devuelto al usuario.",
                        pago.referenciaExterna,
                        pago.usuarioNombre,
                        pago.monto,
                        motivoDevolucion != null && !motivoDevolucion.isBlank() ? motivoDevolucion
                                : "(Sin motivo registrado)"));
                success.showAndWait();

                // Refrescar historial
                cargarHistorialPagos();
            });

        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("✗ No se pudo revertir el pago");
                error.setContentText(e.getMessage());
                error.showAndWait();
            });
        }
    }

    private void editarMotivoDevolucion(PagoEnriquecidoDto pago) {
        // Extraer motivo actual
        String motivoActual = extraerMotivoDevolucion(pago.metadatos);

        TextInputDialog dialog = new TextInputDialog(motivoActual);
        dialog.setTitle("Editar Motivo de Devolución");
        dialog.setHeaderText("Modificar motivo de devolución");
        dialog.setContentText(String.format(
                "Pago: %s\nUsuario: %s\nMonto: $%,.0f\n\nNuevo motivo:",
                pago.referenciaExterna,
                pago.usuarioNombre,
                pago.monto));
        dialog.getEditor().setPrefColumnCount(40);

        dialog.showAndWait().ifPresent(nuevoMotivo -> {
            if (nuevoMotivo != null && !nuevoMotivo.isBlank()) {
                runAsync(() -> actualizarMotivoDevolucion(pago, nuevoMotivo));
            }
        });
    }

    private void actualizarMotivoDevolucion(PagoEnriquecidoDto pago, String nuevoMotivo) {
        try {
            String metadatosActual = pago.metadatos != null ? pago.metadatos : "{}";
            String nuevoJson = mergeMotivoDevolución(metadatosActual, nuevoMotivo);
            api.actualizarMetadatosPago(UUID.fromString(pago.id), nuevoJson);

            Platform.runLater(() -> {
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Motivo Actualizado");
                ok.setHeaderText("✓ Motivo de devolución actualizado");
                ok.setContentText("Se ha guardado el nuevo motivo de devolución.");
                ok.showAndWait();
                cargarHistorialPagos();
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("✗ No se pudo actualizar el motivo");
                err.setContentText(e.getMessage());
                err.showAndWait();
            });
        }
    }

    private String extraerMotivoDevolucion(String metadatos) {
        if (metadatos == null || metadatos.isBlank()) {
            return "";
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(metadatos);
            if (root.has("motivo_devolucion")) {
                return root.get("motivo_devolucion").asText();
            }
        } catch (Exception ignored) {
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("\\\"motivo_devolucion\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        java.util.regex.Matcher matcher = pattern.matcher(metadatos);
        if (matcher.find()) {
            return matcher.group(1).replace("\\n", " ").replace("\\\"", "\"").trim();
        }
        return "";
    }

    private String mergeMotivoDevolución(String jsonActual, String motivo) {
        String seguro = motivo.replace("\\", "\\\\").replace("\"", "\\\"");
        // Si ya contiene la clave, reemplazar su valor
        if (jsonActual.contains("\"motivo_devolucion\"")) {
            return jsonActual.replaceAll("\\\"motivo_devolucion\\\"\\s*:\\s*\\\"[^\\\"]*\\\"",
                    "\\\"motivo_devolucion\\\":\\\"" + seguro + "\\\"");
        }
        // Insertar antes de la última llave
        if (jsonActual.trim().endsWith("}")) {
            if (jsonActual.trim().equals("{}")) {
                return "{\"motivo_devolucion\":\"" + seguro + "\"}";
            }
            String base = jsonActual.trim();
            if (base.length() > 1 && base.charAt(base.length() - 2) != '{') {
                return base.substring(0, base.length() - 1) + ",\"motivo_devolucion\":\"" + seguro + "\"}";
            } else {
                return base.substring(0, base.length() - 1) + "\"motivo_devolucion\":\"" + seguro + "\"}";
            }
        }
        return "{\"motivo_devolucion\":\"" + seguro + "\"}";
    }

    private void showMenu() {
        container.getChildren().setAll(menuView);
        AnimationUtils.fadeIn(menuView);
    }

    private void showHistory() {
        container.getChildren().setAll(historyView);
        AnimationUtils.fadeIn(historyView);
        // Cargar historial automáticamente
        runAsync(() -> cargarHistorialPagos());
    }

    private void showAssignPayment() {
        container.getChildren().setAll(assignView);
        AnimationUtils.fadeIn(assignView);
    }

    private void showCancelInvoice() {
        container.getChildren().setAll(cancelView);
        AnimationUtils.fadeIn(cancelView);
        cancelView.limpiarFormulario();
    }

    private void wireAssignView() {
        // Regresar al menú
        assignView.regresarBtn.setOnAction(e -> showMenu());

        // Búsqueda de usuario
        assignView.buscarBtn.setOnAction(e -> {
            String criterio = assignView.busquedaField.getText().trim();
            String tipoBusqueda = assignView.tipoBusquedaCombo.getValue();

            if (criterio.isEmpty()) {
                assignView.setStatus("⚠ Ingrese un criterio de búsqueda", true);
                return;
            }

            assignView.setStatus("Buscando...", false);
            runAsync(() -> realizarBusqueda(tipoBusqueda, criterio));
        });

        // Asignar pago
        assignView.asignarBtn.setOnAction(e -> {
            try {
                AssignPaymentView.PaymentData data = assignView.getPaymentData();
                assignView.setStatus("Creando pago...", false);
                assignView.asignarBtn.setDisable(true);
                runAsync(() -> crearNuevoPago(data));
            } catch (IllegalArgumentException ex) {
                assignView.setStatus("✗ " + ex.getMessage(), true);
                AnimationUtils.shake(assignView);
            }
        });
    }

    private void realizarBusqueda(String tipo, String criterio) {
        try {
            List<UsuarioDto> resultados;

            switch (tipo) {
                case "Cédula":
                    var opt = api.buscarUsuarioPorDocumento(criterio);
                    resultados = opt.map(List::of).orElse(Collections.emptyList());
                    break;
                case "Nombre":
                    resultados = api.buscarUsuariosPorNombre(criterio);
                    break;
                case "ID":
                    // Buscar por ID exacto - asumiendo que es nombre completo
                    resultados = api.buscarUsuariosPorNombre(criterio);
                    break;
                default:
                    resultados = Collections.emptyList();
            }

            final List<UsuarioDto> finalResultados = resultados;
            Platform.runLater(() -> assignView.setResultados(finalResultados));

        } catch (IOException e) {
            Platform.runLater(() -> assignView.setStatus("✗ Error en búsqueda: " + e.getMessage(), true));
        }
    }

    private void crearNuevoPago(AssignPaymentView.PaymentData data) {
        try {
            if (data.usuarioId() == null || data.usuarioId().isBlank()) {
                throw new IllegalArgumentException("El ID de usuario no puede estar vacío");
            }
            System.out.println("[Cashier] Crear pago -> usuarioId=" + data.usuarioId() +
                    ", monto=" + data.monto() + ", tipo=" + data.tipo() + ", metodo=" + data.metodo() +
                    ", ref=" + data.referencia() + ", diasPaquete=" + data.diasPaquete());
            // Crear solicitud de pago
            // metadatos debe ser JSON válido para Postgres (columna json). Si el motivo es
            // texto plano, lo envolvemos.
            String metadatosJson = data.motivo() == null || data.motivo().isBlank()
                    ? null
                    : toSafeJson(data.motivo());

            PaymentApiClient.CreatePagoRequest req = new PaymentApiClient.CreatePagoRequest(
                    data.usuarioId(),
                    data.monto(),
                    data.tipo(),
                    data.metodo(),
                    data.referencia(),
                    data.diasPaquete(),
                    System.getProperty("user.name", "cajero"),
                    metadatosJson // Guardar motivo envuelto como JSON
            );

            PagoDto creado = api.crearPago(req);

            // Auto-aprobar si está marcado
            if (data.aprobarAuto() && creado != null && creado.id != null && !creado.id.isBlank()) {
                try {
                    creado = api.aprobarPago(UUID.fromString(creado.id));
                } catch (Exception ex) {
                    final String msg = ex.getMessage();
                    Platform.runLater(() -> {
                        assignView.setStatus("⚠ Pago creado pero falla aprobar: " + msg, true);
                        assignView.asignarBtn.setDisable(false);
                    });
                    return;
                }
            }

            final PagoDto pagoFinal = creado;
            Platform.runLater(() -> {
                String estado = pagoFinal.estadoPago != null ? pagoFinal.estadoPago.name() : "CREADO";
                String mensaje = String.format("✓ Pago %s correctamente\nID: %s\nReferencia: %s",
                        estado, pagoFinal.id, data.referencia());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText("Pago Asignado Correctamente");
                alert.setContentText(mensaje);
                alert.showAndWait();

                assignView.setStatus(mensaje, false);
                AnimationUtils.pulse(assignView);
                assignView.limpiarFormulario();
                assignView.asignarBtn.setDisable(false);
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                String mensaje = "✗ Error creando pago: " + e.getMessage();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("No se pudo asignar el pago");
                alert.setContentText(e.getMessage());
                alert.showAndWait();

                assignView.setStatus(mensaje, true);
                AnimationUtils.shake(assignView);
                assignView.asignarBtn.setDisable(false);
            });
        }
    }

    private void wireCancelView() {
        // Regresar al menú
        cancelView.setOnRegresar(this::showMenu);

        // Búsqueda de facturas
        cancelView.setOnBuscarFacturas(dummy -> {
            String criterio = cancelView.getCriterioBusqueda();
            String tipo = cancelView.getTipoBusqueda();

            if (criterio == null || criterio.isBlank()) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    a.setTitle("Búsqueda");
                    a.setHeaderText(null);
                    a.setContentText("Ingrese un criterio de búsqueda");
                    a.showAndWait();
                });
                return;
            }

            runAsync(() -> buscarFacturas(tipo, criterio));
        });

        // Cancelar factura
        cancelView.setOnCancelarFactura(factura -> {
            String motivo = cancelView.getMotivoCancelacion();
            runAsync(() -> cancelarFactura(factura, motivo));
        });

        // Guardar edición de motivo de cancelación
        cancelView.setOnGuardarMotivoCancelacion((factura, nuevoMotivo) -> {
            runAsync(() -> guardarMotivoCancelacion(factura, nuevoMotivo));
        });
    }

    private void buscarFacturas(String tipo, String criterio) {
        Platform.runLater(() -> cancelView.setResultados(Collections.emptyList()));

        try {
            List<PagoEnriquecidoDto> resultados = null;

            switch (tipo) {
                case "Referencia de Factura":
                    resultados = api.buscarFacturaPorReferencia(criterio);
                    break;
                case "Documento Usuario":
                    resultados = api.buscarFacturasPorDocumento(criterio);
                    break;
                case "Nombre Usuario":
                    resultados = api.buscarFacturasPorNombre(criterio);
                    break;
            }

            final List<PagoEnriquecidoDto> finalResults = resultados != null ? resultados : Collections.emptyList();
            Platform.runLater(() -> cancelView.setResultados(finalResults));

        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de búsqueda");
                alert.setHeaderText(null);
                alert.setContentText("Error al buscar facturas: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void cancelarFactura(PagoEnriquecidoDto factura, String motivo) {
        try {
            // Llamar al endpoint de cancelación/revertir
            api.revertirPago(UUID.fromString(factura.id));

            // Guardar motivo de cancelación dentro de metadatos si el usuario escribió algo
            if (motivo != null && !motivo.isBlank()) {
                String metadatosActual = factura.metadatos != null ? factura.metadatos : "{}";
                String nuevoJson = mergeMotivoCancelacion(metadatosActual, motivo);
                api.actualizarMetadatosPago(UUID.fromString(factura.id), nuevoJson);
            }

            Platform.runLater(() -> {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Factura Cancelada");
                success.setHeaderText("✓ Factura cancelada exitosamente");
                success.setContentText(String.format(
                        "La factura %s ha sido cancelada.\nUsuario: %s\nMonto: $%,.0f",
                        factura.referenciaExterna,
                        factura.usuarioNombre,
                        factura.monto));
                success.showAndWait();

                // Refrescar búsqueda
                buscarFacturas(cancelView.getTipoBusqueda(), cancelView.getCriterioBusqueda());
            });

        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText("✗ No se pudo cancelar la factura");
                error.setContentText(e.getMessage());
                error.showAndWait();
                AnimationUtils.shake(cancelView);
            });
        }
    }

    private void guardarMotivoCancelacion(PagoEnriquecidoDto factura, String nuevoMotivo) {
        try {
            if (factura == null)
                return;
            String metadatosActual = factura.metadatos != null ? factura.metadatos : "{}";
            String nuevoJson = mergeMotivoCancelacion(metadatosActual, nuevoMotivo);
            api.actualizarMetadatosPago(UUID.fromString(factura.id), nuevoJson);
            Platform.runLater(() -> {
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Motivo Actualizado");
                ok.setHeaderText("✓ Motivo de cancelación actualizado");
                ok.setContentText("Se guardó el nuevo motivo de cancelación.");
                ok.showAndWait();
                buscarFacturas(cancelView.getTipoBusqueda(), cancelView.getCriterioBusqueda());
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("✗ No se pudo actualizar el motivo");
                err.setContentText(e.getMessage());
                err.showAndWait();
            });
        }
    }

    // Fusiona / inserta motivo_cancelacion dentro del JSON metadatos existente
    private String mergeMotivoCancelacion(String jsonActual, String motivo) {
        String seguro = motivo.replace("\\", "\\\\").replace("\"", "\\\"");
        // Si ya contiene la clave, reemplazar su valor de forma simple (no robusto pero
        // suficiente)
        if (jsonActual.contains("\"motivo_cancelacion\"")) {
            return jsonActual.replaceAll("\\\"motivo_cancelacion\\\"\\s*:\\s*\\\"[^\\\"]*\\\"",
                    "\\\"motivo_cancelacion\\\":\\\"" + seguro + "\\\"");
        }
        // Insertar antes de la última llave si es un objeto simple
        if (jsonActual.trim().endsWith("}")) {
            if (jsonActual.trim().equals("{}")) {
                return "{\"motivo_cancelacion\":\"" + seguro + "\"}";
            }
            // Añadir coma si no hay
            String base = jsonActual.trim();
            if (base.length() > 1 && base.charAt(base.length() - 2) != '{') {
                return base.substring(0, base.length() - 1) + ",\"motivo_cancelacion\":\"" + seguro + "\"}";
            } else {
                return base.substring(0, base.length() - 1) + "\"motivo_cancelacion\":\"" + seguro + "\"}";
            }
        }
        // Si no parece JSON, crear uno nuevo con ambos motivos si existe motivo
        // original
        return "{\"motivo_cancelacion\":\"" + seguro + "\"}";
    }

    private void runAsync(Runnable r) {
        new Thread(r, "cashier-module").start();
    }

    // Convierte un texto plano en un JSON simple {"motivo":"..."} escapando
    // comillas
    private String toSafeJson(String motivo) {
        String escaped = motivo.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"motivo\":\"" + escaped + "\"}";
    }
}
