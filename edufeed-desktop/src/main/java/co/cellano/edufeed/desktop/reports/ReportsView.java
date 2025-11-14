package co.cellano.edufeed.desktop.reports;

import co.cellano.edufeed.desktop.service.PaymentApiClient;
import co.cellano.edufeed.desktop.reports.controllers.FinancialReportsController;
import co.cellano.edufeed.desktop.reports.controllers.PackagesController;
import co.cellano.edufeed.desktop.reports.views.FinancialDashboardView;
import co.cellano.edufeed.desktop.reports.views.PackagesView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/**
 * Vista principal para el módulo de Reportes con arquitectura de tabs.
 * Tab 1: 💰 Económicos - Dashboard financiero completo con métricas, gráficas y
 * exportación PDF/CSV
 * Tab 2: 🎁 Paquetes - Gestión de paquetes de servicios (Lite, Estándar,
 * Premium)
 */
public class ReportsView {
    // Legacy fields - mantenidos para compatibilidad con ReportsController
    // existente
    public final ChoiceBox<String> reportType = new ChoiceBox<>();
    public final DatePicker desde = new DatePicker();
    public final DatePicker hasta = new DatePicker();
    public final Button buscar = new Button("Buscar");
    public final Button exportCsv = new Button("Exportar CSV");
    public final Label resumen = new Label("");
    public final TableView<Object> table = new TableView<>();
    public final Pagination pagination = new Pagination();

    private final BorderPane root = new BorderPane();
    private final TabPane tabPane = new TabPane();

    // Controladores
    private FinancialReportsController financialController;
    private PackagesController packagesController;

    public ReportsView(PaymentApiClient paymentApiClient) {
        configurarTabPane();

        // Tab 1: 💰 Reportes Económicos
        Tab tabEconomicos = crearTabEconomicos(paymentApiClient);

        // Tab 2: 🎁 Paquetes
        Tab tabPaquetes = crearTabPaquetes(paymentApiClient);

        tabPane.getTabs().addAll(tabEconomicos, tabPaquetes);

        // Seleccionar tab Económicos por defecto
        tabPane.getSelectionModel().select(tabEconomicos);

        root.setCenter(tabPane);
        aplicarEstilos();
    }

    private void configurarTabPane() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-width: 150px; -fx-tab-max-width: 200px;");
    }

    /**
     * Crea el tab de Reportes Económicos con FinancialDashboardView y su
     * controlador
     */
    private Tab crearTabEconomicos(PaymentApiClient paymentApiClient) {
        Tab tab = new Tab("💰 Económicos");
        tab.setId("tab-economicos");

        // Contenedor principal con overlay para loading spinner
        StackPane contenedor = new StackPane();

        // Vista del dashboard financiero
        FinancialDashboardView dashboardView = new FinancialDashboardView();

        // Controlador que conecta la vista con los servicios
        financialController = new FinancialReportsController(
                dashboardView,
                paymentApiClient,
                contenedor);

        // Agregar vista al contenedor
        contenedor.getChildren().add(dashboardView);

        // Configurar contenido del tab
        ScrollPane scrollPane = new ScrollPane(contenedor);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #f5f7fa;");

        tab.setContent(scrollPane);

        // Cargar datos iniciales cuando se seleccione el tab por primera vez
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected() && financialController != null) {
                financialController.cargarDatosIniciales();
            }
        });

        return tab;
    }

    /**
     * Crea el tab de Paquetes con la vista de gestión de paquetes de servicios
     */
    private Tab crearTabPaquetes(PaymentApiClient paymentApiClient) {
        Tab tab = new Tab("\uD83C\uDF81 Paquetes"); // 🎁
        tab.setId("tab-paquetes");

        // Contenedor principal con overlay para loading spinner
        StackPane contenedor = new StackPane();

        // Vista de paquetes
        PackagesView packagesView = new PackagesView();

        // Controlador que conecta la vista con los servicios
        packagesController = new PackagesController(
                packagesView,
                paymentApiClient,
                contenedor);

        // Agregar vista al contenedor
        contenedor.getChildren().add(packagesView);

        // Configurar contenido del tab con ScrollPane
        ScrollPane scrollPane = new ScrollPane(contenedor);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #f5f7fa;");

        tab.setContent(scrollPane);

        // Cargar datos iniciales cuando se seleccione el tab por primera vez
        tab.setOnSelectionChanged(event -> {
            if (tab.isSelected() && packagesController != null) {
                packagesController.cargarDatosIniciales();
            }
        });

        return tab;
    }

    private void aplicarEstilos() {
        root.setStyle("-fx-background-color: #f5f7fa;");

        // Estilos para las tabs
        tabPane.setStyle("""
                    -fx-background-color: #ffffff;
                    -fx-tab-min-width: 150px;
                    -fx-tab-max-width: 200px;
                    -fx-tab-min-height: 40px;
                """);

        // CSS para tabs activas/inactivas - cargar si existe
        try {
            var cssUrl = getClass().getResource("/css/reports-tabs.css");
            if (cssUrl != null) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar CSS de tabs: " + e.getMessage());
        }
    }

    public BorderPane getRoot() {
        return root;
    }

    /**
     * Obtiene el controlador del dashboard financiero (útil para testing)
     */
    public FinancialReportsController getFinancialController() {
        return financialController;
    }

    /**
     * Obtiene el controlador de paquetes (útil para testing)
     */
    public PackagesController getPackagesController() {
        return packagesController;
    }

    /**
     * Obtiene el TabPane principal (útil para navegación programática)
     */
    public TabPane getTabPane() {
        return tabPane;
    }
}
