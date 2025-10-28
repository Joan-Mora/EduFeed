package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class BiometricManagementView extends BorderPane {
    final TableView<UserApiClient.PlantillaBiometricaDto> table = new TableView<>();
    final ComboBox<UserApiClient.Modalidad> modalidad = new ComboBox<>();
    final Button enrolarBtn = new Button("Enrolar");
    final Button desactivarBtn = new Button("Desactivar");
    final Label status = new Label();

    public BiometricManagementView() {
        setPadding(new Insets(10));
        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().id));
        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colMod = new TableColumn<>("Modalidad");
        colMod.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().modalidad));
        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colProv = new TableColumn<>("Proveedor");
        colProv.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().proveedor));
        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colCre = new TableColumn<>("Creado en");
        colCre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().creadoEn));
        TableColumn<UserApiClient.PlantillaBiometricaDto, String> colAct = new TableColumn<>("Activo");
        colAct.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(Boolean.TRUE.equals(c.getValue().activo)?"Sí":"No"));
        table.getColumns().addAll(colId, colMod, colProv, colCre, colAct);
    // Para compatibilidad amplia con versiones de JavaFX, usar la política clásica
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setCenter(table);

        modalidad.getItems().addAll(UserApiClient.Modalidad.HUELLA, UserApiClient.Modalidad.ROSTRO, UserApiClient.Modalidad.VOZ);
        modalidad.setValue(UserApiClient.Modalidad.HUELLA);
        HBox actions = new HBox(8, new Label("Modalidad:"), modalidad, enrolarBtn, desactivarBtn);
        BorderPane bottom = new BorderPane(); bottom.setLeft(actions); bottom.setRight(status);
        bottom.setPadding(new Insets(8,0,0,0)); setBottom(bottom);
    }
}
