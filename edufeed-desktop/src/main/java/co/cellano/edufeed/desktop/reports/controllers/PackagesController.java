package co.cellano.edufeed.desktop.reports.controllers;

import co.cellano.edufeed.desktop.reports.models.PaqueteAsignadoDto;
import co.cellano.edufeed.desktop.reports.services.PackageService;
import co.cellano.edufeed.desktop.reports.views.AssignPackageDialog;
import co.cellano.edufeed.desktop.reports.views.PackagesView;
import co.cellano.edufeed.desktop.service.PaymentApiClient;
import co.cellano.edufeed.desktop.service.PaymentApiClient.UsuarioDto;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador para la vista de paquetes.
 * Gestiona la lógica de negocio, búsqueda de usuarios y asignación de paquetes.
 */
public class PackagesController {

    private final PackagesView view;
    private final PackageService packageService;
    private final PaymentApiClient paymentApiClient;
    private final StackPane contenedor;
    private final ProgressIndicator spinner;

    private List<PaqueteAsignadoDto> paquetesActuales;

    public PackagesController(PackagesView view, PaymentApiClient paymentApiClient, StackPane contenedor) {
        this.view = view;
        this.paymentApiClient = paymentApiClient;
        this.packageService = new PackageService(paymentApiClient);
        this.contenedor = contenedor;

        // Spinner de carga
        this.spinner = new ProgressIndicator();
        this.spinner.setPrefSize(80, 80);
        this.spinner.setStyle("-fx-progress-color: #007bff;");
        this.spinner.setVisible(false);

        configurarCallbacks();
    }

    private void configurarCallbacks() {
        view.setOnBuscar(filtros -> buscarPaquetes(filtros));
        view.setOnAsignarPaquete(() -> mostrarDialogoAsignacion());
    }

    /**
     * Carga los datos iniciales al abrir la vista.
     */
    public void cargarDatosIniciales() {
        if (paquetesActuales != null && !paquetesActuales.isEmpty()) {
            // Ya hay datos cargados, no recargar
            return;
        }

        mostrarSpinner(true);

        CompletableFuture.runAsync(() -> {
            try {
                List<PaqueteAsignadoDto> paquetes = packageService.listarPaquetesAsignados();

                Platform.runLater(() -> {
                    paquetesActuales = paquetes;
                    view.actualizarTabla(paquetes);
                    mostrarSpinner(false);
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarSpinner(false);
                    mostrarError("Error al cargar paquetes",
                            "No se pudieron cargar los paquetes: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Busca paquetes aplicando los filtros seleccionados.
     */
    private void buscarPaquetes(PackagesView.FiltrosPaquetes filtros) {
        mostrarSpinner(true);

        CompletableFuture.runAsync(() -> {
            try {
                // Cargar todos los paquetes si no están cargados
                if (paquetesActuales == null) {
                    paquetesActuales = packageService.listarPaquetesAsignados();
                }

                // Aplicar filtros
                List<PaqueteAsignadoDto> paquetesFiltrados = packageService.filtrarPaquetes(
                        paquetesActuales,
                        filtros.fechaDesde,
                        filtros.fechaHasta,
                        filtros.documentoUsuario,
                        filtros.nombreUsuario,
                        filtros.idUsuario,
                        filtros.estados);

                Platform.runLater(() -> {
                    view.actualizarTabla(paquetesFiltrados);
                    mostrarSpinner(false);
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarSpinner(false);
                    mostrarError("Error en búsqueda",
                            "No se pudo realizar la búsqueda: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Muestra el diálogo para asignar un nuevo paquete.
     */
    private void mostrarDialogoAsignacion() {
        AssignPackageDialog dialogo = new AssignPackageDialog();

        // Configurar búsqueda de usuario
        dialogo.getBtnBuscarUsuario().setOnAction(e -> {
            String documento = dialogo.getDocumento();
            if (documento.isEmpty()) {
                mostrarAdvertencia("Documento requerido",
                        "Por favor ingrese un número de documento para buscar.");
                return;
            }

            buscarUsuarioPorDocumento(documento, dialogo);
        });

        // Configurar callback de confirmación
        dialogo.setOnConfirmar(resultado -> {
            asignarPaquete(resultado, dialogo);
        });

        dialogo.showAndWait();
    }

    /**
     * Busca un usuario por documento y actualiza el diálogo.
     */
    private void buscarUsuarioPorDocumento(String documento, AssignPackageDialog dialogo) {
        CompletableFuture.runAsync(() -> {
            try {
                Optional<UsuarioDto> usuarioOpt = paymentApiClient.buscarUsuarioPorDocumento(documento);

                Platform.runLater(() -> {
                    dialogo.setUsuarioEncontrado(usuarioOpt.orElse(null));
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    dialogo.setUsuarioEncontrado(null);
                    mostrarError("Error al buscar usuario",
                            "No se pudo buscar el usuario: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Asigna el paquete al usuario seleccionado.
     */
    private void asignarPaquete(AssignPackageDialog.ResultadoAsignacion resultado, AssignPackageDialog dialogo) {
        mostrarSpinner(true);

        CompletableFuture.runAsync(() -> {
            try {
                PaymentApiClient.PagoDto pagoCreado = packageService.asignarPaquete(
                        resultado.usuario.id,
                        resultado.paquete,
                        resultado.metodoPago,
                        resultado.referencia,
                        resultado.cajero);

                Platform.runLater(() -> {
                    mostrarSpinner(false);
                    mostrarExito("Paquete asignado",
                            String.format(
                                    "Se ha asignado el paquete '%s' a %s.\n\n" +
                                            "ID del Pago: %s\n" +
                                            "Monto: $%,d\n" +
                                            "Estado: PENDIENTE\n\n" +
                                            "El usuario deberá cancelar este pago en caja.",
                                    resultado.paquete.getNombre(),
                                    resultado.usuario.nombreCompleto,
                                    pagoCreado.id,
                                    resultado.paquete.getCosto().intValue()));

                    // Recargar datos
                    paquetesActuales = null; // Forzar recarga
                    cargarDatosIniciales();
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    mostrarSpinner(false);
                    mostrarError("Error al asignar paquete",
                            "No se pudo asignar el paquete: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Muestra/oculta el spinner de carga.
     */
    private void mostrarSpinner(boolean mostrar) {
        if (contenedor != null) {
            spinner.setVisible(mostrar);
            if (mostrar && !contenedor.getChildren().contains(spinner)) {
                contenedor.getChildren().add(spinner);
            }
        }
    }

    /**
     * Muestra un mensaje de error.
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un mensaje de advertencia.
     */
    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un mensaje de éxito.
     */
    private void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
