package co.cellano.edufeed.desktop.reports.views;

import co.cellano.edufeed.desktop.reports.models.PaqueteServicio;
import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Diálogo para asignar un nuevo paquete a un usuario.
 * Permite buscar usuario, seleccionar paquete y método de pago.
 */
public class AssignPackageDialog extends Stage {

    private final TextField txtDocumento;
    private final Button btnBuscarUsuario;
    private final Label lblUsuarioEncontrado;
    private final ComboBox<PaqueteServicio> cbPaquete;
    private final ComboBox<String> cbMetodoPago;
    private final TextField txtReferencia;
    private final TextField txtCajero;
    private final TextArea txtDetallesPaquete;
    private final Button btnAsignar;
    private final Button btnCancelar;

    private UsuarioDto usuarioSeleccionado;
    private Consumer<ResultadoAsignacion> onConfirmar;

    public AssignPackageDialog() {
        // Configurar ventana
        this.setTitle("Asignar Paquete de Servicios");
        this.initModality(Modality.APPLICATION_MODAL);
        this.setResizable(false);

        // Inicializar componentes
        txtDocumento = new TextField();
        btnBuscarUsuario = new Button("\uD83D\uDD0D Buscar"); // 🔍
        lblUsuarioEncontrado = new Label("Busque un usuario por documento");
        cbPaquete = new ComboBox<>();
        cbMetodoPago = new ComboBox<>();
        txtReferencia = new TextField();
        txtCajero = new TextField();
        txtDetallesPaquete = new TextArea();
        btnAsignar = new Button("\u2713 Asignar Paquete"); // ✓
        btnCancelar = new Button("\u2716 Cancelar"); // ✖

        construirUI();
        configurarEventos();
    }

    private void construirUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #f8f9fa;");
        root.setPrefWidth(600);

        // Header
        VBox header = construirHeader();

        // Sección 1: Búsqueda de Usuario
        VBox seccionUsuario = construirSeccionUsuario();

        // Sección 2: Selección de Paquete
        VBox seccionPaquete = construirSeccionPaquete();

        // Sección 3: Detalles de Pago
        VBox seccionPago = construirSeccionPago();

        // Botones de acción
        HBox botones = construirBotones();

        root.getChildren().addAll(header, seccionUsuario, seccionPaquete, seccionPago, botones);

        Scene scene = new Scene(root);
        this.setScene(scene);
    }

    private VBox construirHeader() {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label titulo = new Label("\uD83C\uDF81 Asignación de Paquete"); // 🎁
        titulo.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #2c3e50;");

        Text descripcion = new Text(
                "Complete los siguientes campos para asignar un paquete de servicios a un usuario. " +
                        "El paquete se creará como un pago PENDIENTE que el usuario deberá cancelar en caja.");
        descripcion.setWrappingWidth(550);
        descripcion.setStyle("-fx-fill: #6c757d; -fx-font-size: 12px;");

        header.getChildren().addAll(titulo, descripcion);

        return header;
    }

    private VBox construirSeccionUsuario() {
        VBox seccion = new VBox(10);
        seccion.setPadding(new Insets(15));
        seccion.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;");

        Label lblTitulo = new Label("1. Buscar Usuario");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        HBox busqueda = new HBox(10);
        busqueda.setAlignment(Pos.CENTER_LEFT);

        Label lblDocumento = new Label("Documento:");
        lblDocumento.setStyle("-fx-font-weight: bold;");

        txtDocumento.setPromptText("Ej: 1234567890");
        txtDocumento.setPrefWidth(200);

        btnBuscarUsuario.setStyle(
                "-fx-background-color: #007bff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 15;" +
                        "-fx-background-radius: 5;" +
                        "-fx-cursor: hand;");

        busqueda.getChildren().addAll(lblDocumento, txtDocumento, btnBuscarUsuario);

        lblUsuarioEncontrado.setStyle(
                "-fx-padding: 10;" +
                        "-fx-background-color: #e9ecef;" +
                        "-fx-background-radius: 5;" +
                        "-fx-text-fill: #6c757d;" +
                        "-fx-font-style: italic;");
        lblUsuarioEncontrado.setWrapText(true);
        lblUsuarioEncontrado.setMaxWidth(Double.MAX_VALUE);

        seccion.getChildren().addAll(lblTitulo, busqueda, lblUsuarioEncontrado);

        return seccion;
    }

    private VBox construirSeccionPaquete() {
        VBox seccion = new VBox(10);
        seccion.setPadding(new Insets(15));
        seccion.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;");

        Label lblTitulo = new Label("2. Seleccionar Paquete");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        HBox seleccion = new HBox(10);
        seleccion.setAlignment(Pos.CENTER_LEFT);

        Label lblPaquete = new Label("Paquete:");
        lblPaquete.setStyle("-fx-font-weight: bold;");

        cbPaquete.setItems(FXCollections.observableArrayList(PaqueteServicio.values()));
        cbPaquete.setPromptText("Seleccione un paquete");
        cbPaquete.setPrefWidth(350);

        seleccion.getChildren().addAll(lblPaquete, cbPaquete);

        txtDetallesPaquete.setPrefRowCount(4);
        txtDetallesPaquete.setEditable(false);
        txtDetallesPaquete.setWrapText(true);
        txtDetallesPaquete.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-font-size: 12px;");
        txtDetallesPaquete.setPromptText("Seleccione un paquete para ver los detalles...");

        seccion.getChildren().addAll(lblTitulo, seleccion, txtDetallesPaquete);

        return seccion;
    }

    private VBox construirSeccionPago() {
        VBox seccion = new VBox(10);
        seccion.setPadding(new Insets(15));
        seccion.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;");

        Label lblTitulo = new Label("3. Detalles de Pago");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Método de pago
        Label lblMetodo = new Label("Método Pago:");
        lblMetodo.setStyle("-fx-font-weight: bold;");

        cbMetodoPago.setItems(FXCollections.observableArrayList(
                "EFECTIVO", "TARJETA", "TRANSFERENCIA", "POS"));
        cbMetodoPago.setValue("EFECTIVO");
        cbMetodoPago.setMaxWidth(Double.MAX_VALUE);

        // Referencia
        Label lblReferencia = new Label("Referencia:");
        lblReferencia.setStyle("-fx-font-weight: bold;");

        txtReferencia.setPromptText("Opcional: número de transacción, recibo, etc.");
        txtReferencia.setMaxWidth(Double.MAX_VALUE);

        // Cajero
        Label lblCajero = new Label("Cajero:");
        lblCajero.setStyle("-fx-font-weight: bold;");

        txtCajero.setPromptText("Nombre del cajero que asigna el paquete");
        txtCajero.setMaxWidth(Double.MAX_VALUE);

        grid.add(lblMetodo, 0, 0);
        grid.add(cbMetodoPago, 1, 0);
        grid.add(lblReferencia, 0, 1);
        grid.add(txtReferencia, 1, 1);
        grid.add(lblCajero, 0, 2);
        grid.add(txtCajero, 1, 2);

        GridPane.setHgrow(cbMetodoPago, Priority.ALWAYS);
        GridPane.setHgrow(txtReferencia, Priority.ALWAYS);
        GridPane.setHgrow(txtCajero, Priority.ALWAYS);

        seccion.getChildren().addAll(lblTitulo, grid);

        return seccion;
    }

    private HBox construirBotones() {
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10, 0, 0, 0));

        btnAsignar.setStyle(
                "-fx-background-color: #28a745;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 25;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;");
        btnAsignar.setDisable(true); // Deshabilitar hasta que se seleccione usuario y paquete

        btnCancelar.setStyle(
                "-fx-background-color: #6c757d;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 25;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;");

        botones.getChildren().addAll(btnCancelar, btnAsignar);

        return botones;
    }

    private void configurarEventos() {
        // Al seleccionar paquete, mostrar detalles
        cbPaquete.setOnAction(e -> {
            PaqueteServicio paquete = cbPaquete.getValue();
            if (paquete != null) {
                mostrarDetallesPaquete(paquete);
                validarFormulario();
            }
        });

        btnCancelar.setOnAction(e -> this.close());

        btnAsignar.setOnAction(e -> {
            if (validarFormulario()) {
                confirmarAsignacion();
            }
        });
    }

    private void mostrarDetallesPaquete(PaqueteServicio paquete) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCCB Descripción: ").append(paquete.getDescripcion()).append("\n\n"); // 📋
        sb.append("\u23F0 Duración: ").append(paquete.getDuracionDias()).append(" días\n"); // ⏰
        sb.append("\uD83D\uDCB0 Costo: $").append(String.format("%,d", paquete.getCosto().intValue())).append("\n\n"); // 💰
        sb.append("\u2728 Beneficios:\n"); // ✨
        for (String beneficio : paquete.getBeneficios()) {
            sb.append("  • ").append(beneficio).append("\n");
        }

        txtDetallesPaquete.setText(sb.toString());
    }

    private boolean validarFormulario() {
        boolean valido = usuarioSeleccionado != null && cbPaquete.getValue() != null;
        btnAsignar.setDisable(!valido);
        return valido;
    }

    private void confirmarAsignacion() {
        // Mostrar confirmación
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Asignación");
        confirmacion.setHeaderText("¿Está seguro de asignar este paquete?");

        PaqueteServicio paquete = cbPaquete.getValue();
        String mensaje = String.format(
                "Usuario: %s (%s)\n" +
                        "Paquete: %s\n" +
                        "Costo: $%,d\n" +
                        "Método de Pago: %s\n\n" +
                        "Se creará un pago PENDIENTE que el usuario deberá cancelar en caja.",
                usuarioSeleccionado.nombreCompleto,
                usuarioSeleccionado.documento,
                paquete.getNombre(),
                paquete.getCosto().intValue(),
                cbMetodoPago.getValue());

        confirmacion.setContentText(mensaje);

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            // Crear resultado y cerrar
            ResultadoAsignacion resultado2 = new ResultadoAsignacion();
            resultado2.usuario = usuarioSeleccionado;
            resultado2.paquete = paquete;
            resultado2.metodoPago = cbMetodoPago.getValue();
            resultado2.referencia = txtReferencia.getText().trim();
            resultado2.cajero = txtCajero.getText().trim();

            if (onConfirmar != null) {
                onConfirmar.accept(resultado2);
            }

            this.close();
        }
    }

    /**
     * Establece el usuario encontrado después de la búsqueda.
     */
    public void setUsuarioEncontrado(UsuarioDto usuario) {
        this.usuarioSeleccionado = usuario;
        if (usuario != null) {
            lblUsuarioEncontrado.setText(
                    String.format("\u2713 Usuario encontrado: %s (%s) - %s", // ✓
                            usuario.nombreCompleto,
                            usuario.documento,
                            usuario.email != null ? usuario.email : "Sin email"));
            lblUsuarioEncontrado.setStyle(
                    "-fx-padding: 10;" +
                            "-fx-background-color: #d4edda;" +
                            "-fx-background-radius: 5;" +
                            "-fx-text-fill: #155724;" +
                            "-fx-font-weight: bold;");
        } else {
            lblUsuarioEncontrado.setText("\u2716 Usuario no encontrado. Verifique el documento."); // ✖
            lblUsuarioEncontrado.setStyle(
                    "-fx-padding: 10;" +
                            "-fx-background-color: #f8d7da;" +
                            "-fx-background-radius: 5;" +
                            "-fx-text-fill: #721c24;" +
                            "-fx-font-weight: bold;");
        }
        validarFormulario();
    }

    /**
     * Retorna el botón de buscar usuario para conectar con el controlador.
     */
    public Button getBtnBuscarUsuario() {
        return btnBuscarUsuario;
    }

    /**
     * Retorna el campo de documento.
     */
    public String getDocumento() {
        return txtDocumento.getText().trim();
    }

    /**
     * Establece el callback cuando se confirma la asignación.
     */
    public void setOnConfirmar(Consumer<ResultadoAsignacion> callback) {
        this.onConfirmar = callback;
    }

    /**
     * Clase interna para encapsular el resultado de la asignación.
     */
    public static class ResultadoAsignacion {
        public UsuarioDto usuario;
        public PaqueteServicio paquete;
        public String metodoPago;
        public String referencia;
        public String cajero;
    }
}
