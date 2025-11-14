package co.cellano.edufeed.desktop.reports.views;

import co.cellano.edufeed.desktop.reports.models.PaqueteAsignadoDto;
import co.cellano.edufeed.desktop.reports.models.PaqueteServicio;
import co.cellano.edufeed.desktop.service.PaymentApiClient.EstadoPago;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Vista para gestionar paquetes de servicios.
 * Permite filtrar, listar y asignar paquetes a usuarios.
 */
public class PackagesView extends BorderPane {

    // Componentes de filtros
    private final DatePicker dpFechaDesde;
    private final DatePicker dpFechaHasta;
    private final TextField txtDocumento;
    private final TextField txtNombre;
    private final TextField txtIdUsuario;
    private final ComboBox<String> cbEstado;
    private final Button btnBuscar;
    private final Button btnLimpiar;

    // Componentes de asignación
    private final Button btnAsignarPaquete;

    // Tabla de paquetes
    private final TableView<PaqueteAsignadoDto> tablaPaquetes;
    private final ObservableList<PaqueteAsignadoDto> datosPaquetes;

    // Labels de resumen
    private final Label lblTotalPaquetes;
    private final Label lblTotalAprobados;
    private final Label lblTotalPendientes;

    // Callbacks para el controlador
    private Consumer<FiltrosPaquetes> onBuscar;
    private Runnable onAsignarPaquete;

    public PackagesView() {
        // Inicializar componentes
        dpFechaDesde = new DatePicker();
        dpFechaHasta = new DatePicker();
        txtDocumento = new TextField();
        txtNombre = new TextField();
        txtIdUsuario = new TextField();
        cbEstado = new ComboBox<>();
        btnBuscar = new Button("\uD83D\uDD0D Buscar"); // 🔍
        btnLimpiar = new Button("\u2716 Limpiar"); // ✖
        btnAsignarPaquete = new Button("\u2795 Asignar Paquete"); // ➕

        tablaPaquetes = new TableView<>();
        datosPaquetes = FXCollections.observableArrayList();
        tablaPaquetes.setItems(datosPaquetes);

        lblTotalPaquetes = new Label("0");
        lblTotalAprobados = new Label("0");
        lblTotalPendientes = new Label("0");

        construirUI();
        configurarEventos();
        aplicarEstilos();
    }

    private void construirUI() {
        VBox contenedor = new VBox(20);
        contenedor.setPadding(new Insets(20));
        contenedor.setStyle("-fx-background-color: #f8f9fa;");

        // Header
        HBox header = construirHeader();

        // Panel de filtros
        VBox panelFiltros = construirPanelFiltros();

        // Panel de resumen
        HBox panelResumen = construirPanelResumen();

        // Tabla de paquetes
        VBox panelTabla = construirPanelTabla();

        contenedor.getChildren().addAll(header, panelFiltros, panelResumen, panelTabla);
        VBox.setVgrow(panelTabla, Priority.ALWAYS);

        this.setCenter(contenedor);
    }

    private HBox construirHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label titulo = new Label("\uD83C\uDF81 Gestión de Paquetes"); // 🎁
        titulo.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #2c3e50;");

        Text descripcion = new Text(
                "Administra paquetes de servicios: asigna nuevos paquetes a usuarios, " +
                        "filtra por estado (Aprobados, Pendientes, Rechazados, Devoluciones) y " +
                        "realiza seguimiento de pagos.");
        descripcion.setWrappingWidth(600);
        descripcion.setStyle("-fx-fill: #6c757d; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnAsignarPaquete.setStyle(
                "-fx-background-color: #28a745;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;");

        VBox textos = new VBox(5, titulo, descripcion);
        header.getChildren().addAll(textos, spacer, btnAsignarPaquete);

        return header;
    }

    private VBox construirPanelFiltros() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label lblTitulo = new Label("\uD83D\uDCC5 Filtros de Búsqueda"); // 📅
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        // Fila 1: Fechas
        grid.add(crearLabel("Fecha Desde:"), 0, 0);
        grid.add(dpFechaDesde, 1, 0);
        grid.add(crearLabel("Fecha Hasta:"), 2, 0);
        grid.add(dpFechaHasta, 3, 0);

        // Fila 2: Documento y Nombre
        grid.add(crearLabel("Documento:"), 0, 1);
        grid.add(txtDocumento, 1, 1);
        grid.add(crearLabel("Nombre Usuario:"), 2, 1);
        grid.add(txtNombre, 3, 1);

        // Fila 3: ID Usuario y Estado
        grid.add(crearLabel("ID Usuario:"), 0, 2);
        grid.add(txtIdUsuario, 1, 2);
        grid.add(crearLabel("Estado:"), 2, 2);
        grid.add(cbEstado, 3, 2);

        // Configurar componentes
        dpFechaDesde.setPromptText("Seleccione fecha");
        dpFechaHasta.setPromptText("Seleccione fecha");
        txtDocumento.setPromptText("Ej: 1234567890");
        txtNombre.setPromptText("Ej: Juan Pérez");
        txtIdUsuario.setPromptText("UUID del usuario");

        cbEstado.setItems(FXCollections.observableArrayList(
                "Todos", "Aprobados", "Pendientes", "Rechazados", "Devoluciones"));
        cbEstado.setValue("Todos");
        cbEstado.setMaxWidth(Double.MAX_VALUE);

        // Establecer anchos
        dpFechaDesde.setMaxWidth(Double.MAX_VALUE);
        dpFechaHasta.setMaxWidth(Double.MAX_VALUE);
        txtDocumento.setMaxWidth(Double.MAX_VALUE);
        txtNombre.setMaxWidth(Double.MAX_VALUE);
        txtIdUsuario.setMaxWidth(Double.MAX_VALUE);

        GridPane.setHgrow(dpFechaDesde, Priority.ALWAYS);
        GridPane.setHgrow(dpFechaHasta, Priority.ALWAYS);
        GridPane.setHgrow(txtDocumento, Priority.ALWAYS);
        GridPane.setHgrow(txtNombre, Priority.ALWAYS);
        GridPane.setHgrow(txtIdUsuario, Priority.ALWAYS);
        GridPane.setHgrow(cbEstado, Priority.ALWAYS);

        // Botones de acción
        HBox botones = new HBox(10, btnBuscar, btnLimpiar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        btnBuscar.setStyle(
                "-fx-background-color: #007bff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;");

        btnLimpiar.setStyle(
                "-fx-background-color: #6c757d;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 20;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;");

        panel.getChildren().addAll(lblTitulo, grid, botones);

        return panel;
    }

    private HBox construirPanelResumen() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(15));
        panel.setAlignment(Pos.CENTER);
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        VBox cardTotal = crearCardResumen("Total Paquetes", lblTotalPaquetes, "#007bff");
        VBox cardAprobados = crearCardResumen("Aprobados", lblTotalAprobados, "#28a745");
        VBox cardPendientes = crearCardResumen("Pendientes", lblTotalPendientes, "#ffc107");

        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardAprobados, Priority.ALWAYS);
        HBox.setHgrow(cardPendientes, Priority.ALWAYS);

        panel.getChildren().addAll(cardTotal, cardAprobados, cardPendientes);

        return panel;
    }

    private VBox crearCardResumen(String titulo, Label valor, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: normal;");

        valor.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;");

        card.getChildren().addAll(lblTitulo, valor);

        return card;
    }

    private VBox construirPanelTabla() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label lblTitulo = new Label("\uD83D\uDCCB Paquetes Asignados"); // 📋
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        configurarTabla();

        tablaPaquetes.setPrefHeight(500);
        tablaPaquetes.setMinHeight(400);
        VBox.setVgrow(tablaPaquetes, Priority.ALWAYS);

        panel.getChildren().addAll(lblTitulo, tablaPaquetes);

        return panel;
    }

    private void configurarTabla() {
        // Columna Fecha
        TableColumn<PaqueteAsignadoDto, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setPrefWidth(120);
        colFecha.setCellValueFactory(data -> {
            if (data.getValue().getFechaCreacion() != null) {
                String fecha = data.getValue().getFechaCreacion()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                return new javafx.beans.property.SimpleStringProperty(fecha);
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Columna Documento
        TableColumn<PaqueteAsignadoDto, String> colDocumento = new TableColumn<>("Documento");
        colDocumento.setPrefWidth(120);
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("usuarioDocumento"));

        // Columna Nombre
        TableColumn<PaqueteAsignadoDto, String> colNombre = new TableColumn<>("Nombre Usuario");
        colNombre.setPrefWidth(200);
        colNombre.setCellValueFactory(new PropertyValueFactory<>("usuarioNombre"));

        // Columna Paquete
        TableColumn<PaqueteAsignadoDto, String> colPaquete = new TableColumn<>("Paquete");
        colPaquete.setPrefWidth(150);
        colPaquete.setCellValueFactory(data -> {
            String nombre = data.getValue().getNombrePaquete();
            return new javafx.beans.property.SimpleStringProperty(nombre);
        });

        // Columna Monto
        TableColumn<PaqueteAsignadoDto, String> colMonto = new TableColumn<>("Monto");
        colMonto.setPrefWidth(120);
        colMonto.setCellValueFactory(data -> {
            BigDecimal monto = data.getValue().getMonto();
            String texto = monto != null ? "$" + String.format("%,.0f", monto.doubleValue()) : "$0";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });
        colMonto.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Columna Días
        TableColumn<PaqueteAsignadoDto, Integer> colDias = new TableColumn<>("Días");
        colDias.setPrefWidth(80);
        colDias.setCellValueFactory(new PropertyValueFactory<>("diasPaquete"));
        colDias.setStyle("-fx-alignment: CENTER;");

        // Columna Estado
        TableColumn<PaqueteAsignadoDto, String> colEstado = new TableColumn<>("Estado");
        colEstado.setPrefWidth(120);
        colEstado.setCellValueFactory(data -> {
            String estado = data.getValue().getEstadoFormateado();
            return new javafx.beans.property.SimpleStringProperty(estado);
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "Aprobado" -> "-fx-background-color: #d4edda; -fx-text-fill: #155724;";
                        case "Pendiente" -> "-fx-background-color: #fff3cd; -fx-text-fill: #856404;";
                        case "Rechazado" -> "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;";
                        case "Devolución" -> "-fx-background-color: #d1ecf1; -fx-text-fill: #0c5460;";
                        default -> "";
                    };
                    setStyle(color + " -fx-alignment: CENTER; -fx-font-weight: bold; -fx-padding: 5;");
                }
            }
        });

        // Columna Método Pago
        TableColumn<PaqueteAsignadoDto, String> colMetodo = new TableColumn<>("Método Pago");
        colMetodo.setPrefWidth(120);
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));

        // Columna Referencia
        TableColumn<PaqueteAsignadoDto, String> colReferencia = new TableColumn<>("Referencia");
        colReferencia.setPrefWidth(150);
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referenciaExterna"));

        tablaPaquetes.getColumns().addAll(
                colFecha, colDocumento, colNombre, colPaquete, colMonto,
                colDias, colEstado, colMetodo, colReferencia);

        tablaPaquetes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaPaquetes.setPlaceholder(new Label("No hay paquetes para mostrar"));
    }

    private Label crearLabel(String texto) {
        Label label = new Label(texto);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
        return label;
    }

    private void configurarEventos() {
        btnBuscar.setOnAction(e -> {
            if (onBuscar != null) {
                FiltrosPaquetes filtros = obtenerFiltros();
                onBuscar.accept(filtros);
            }
        });

        btnLimpiar.setOnAction(e -> limpiarFiltros());

        btnAsignarPaquete.setOnAction(e -> {
            if (onAsignarPaquete != null) {
                onAsignarPaquete.run();
            }
        });
    }

    private void aplicarEstilos() {
        tablaPaquetes.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-radius: 8;");
    }

    /**
     * Obtiene los filtros actuales del formulario.
     */
    public FiltrosPaquetes obtenerFiltros() {
        FiltrosPaquetes filtros = new FiltrosPaquetes();
        filtros.fechaDesde = dpFechaDesde.getValue();
        filtros.fechaHasta = dpFechaHasta.getValue();
        filtros.documentoUsuario = txtDocumento.getText().trim();
        filtros.nombreUsuario = txtNombre.getText().trim();

        String idTexto = txtIdUsuario.getText().trim();
        if (!idTexto.isEmpty()) {
            try {
                filtros.idUsuario = UUID.fromString(idTexto);
            } catch (IllegalArgumentException ex) {
                // ID inválido, ignorar
            }
        }

        String estadoSeleccionado = cbEstado.getValue();
        if (estadoSeleccionado != null && !estadoSeleccionado.equals("Todos")) {
            filtros.estados = List.of(convertirEstado(estadoSeleccionado));
        }

        return filtros;
    }

    /**
     * Limpia todos los filtros.
     */
    public void limpiarFiltros() {
        dpFechaDesde.setValue(null);
        dpFechaHasta.setValue(null);
        txtDocumento.clear();
        txtNombre.clear();
        txtIdUsuario.clear();
        cbEstado.setValue("Todos");
    }

    /**
     * Actualiza la tabla con nuevos datos.
     */
    public void actualizarTabla(List<PaqueteAsignadoDto> paquetes) {
        datosPaquetes.clear();
        datosPaquetes.addAll(paquetes);

        // Actualizar resumen
        long total = paquetes.size();
        long aprobados = paquetes.stream().filter(p -> p.getEstado() == EstadoPago.APROBADO).count();
        long pendientes = paquetes.stream().filter(p -> p.getEstado() == EstadoPago.PENDIENTE).count();

        lblTotalPaquetes.setText(String.valueOf(total));
        lblTotalAprobados.setText(String.valueOf(aprobados));
        lblTotalPendientes.setText(String.valueOf(pendientes));
    }

    /**
     * Convierte el texto del ComboBox a EstadoPago.
     */
    private EstadoPago convertirEstado(String texto) {
        return switch (texto) {
            case "Aprobados" -> EstadoPago.APROBADO;
            case "Pendientes" -> EstadoPago.PENDIENTE;
            case "Rechazados" -> EstadoPago.RECHAZADO;
            case "Devoluciones" -> EstadoPago.REVERTIDO;
            default -> null;
        };
    }

    // Setters para callbacks
    public void setOnBuscar(Consumer<FiltrosPaquetes> callback) {
        this.onBuscar = callback;
    }

    public void setOnAsignarPaquete(Runnable callback) {
        this.onAsignarPaquete = callback;
    }

    /**
     * Clase interna para encapsular filtros de búsqueda.
     */
    public static class FiltrosPaquetes {
        public LocalDate fechaDesde;
        public LocalDate fechaHasta;
        public String documentoUsuario;
        public String nombreUsuario;
        public UUID idUsuario;
        public List<EstadoPago> estados;
    }
}
