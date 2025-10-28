package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;


public class UserFormView extends GridPane {
    public interface OnSubmit { void accept(UserApiClient.UsuarioDto dto); }

    final TextField documento = new TextField();
    final TextField nombre = new TextField();
    final ComboBox<String> tipo = new ComboBox<>();
    final TextField email = new TextField();
    final TextField telefono = new TextField();
    final CheckBox activo = new CheckBox("Activo");
    final Button guardar = new Button("Guardar");
    final Label status = new Label();

    public UserFormView(OnSubmit handler) {
        setHgap(10); setVgap(8); setPadding(new Insets(10));
        tipo.getItems().addAll("NINO","ESTUDIANTE","DOCENTE","PERSONAL");
        activo.setSelected(true);

        add(new Label("Documento"), 0, 0); add(documento, 1, 0);
        add(new Label("Nombre completo"), 0, 1); add(nombre, 1, 1);
        add(new Label("Tipo"), 0, 2); add(tipo, 1, 2);
        add(new Label("Email"), 0, 3); add(email, 1, 3);
        add(new Label("Teléfono"), 0, 4); add(telefono, 1, 4);
        add(activo, 1, 5);
        add(guardar, 1, 6);
        add(status, 1, 7);

        guardar.setOnAction(e -> {
            String msg = validate();
            if (msg != null) { status.setText(msg); return; }
            UserApiClient.UsuarioDto dto = new UserApiClient.UsuarioDto();
            dto.documento = documento.getText().trim();
            dto.nombreCompleto = nombre.getText().trim();
            dto.tipoUsuario = tipo.getValue();
            dto.email = email.getText().trim();
            dto.telefono = telefono.getText().trim();
            dto.activo = activo.isSelected();
            handler.accept(dto);
        });
    }

    public void load(UserApiClient.UsuarioDto dto) {
        if (dto == null) return;
        documento.setText(dto.documento);
        nombre.setText(dto.nombreCompleto);
        tipo.setValue(dto.tipoUsuario);
        email.setText(dto.email);
        telefono.setText(dto.telefono);
        activo.setSelected(Boolean.TRUE.equals(dto.activo));
    }

    private String validate() {
        if (documento.getText()==null || documento.getText().trim().isEmpty()) return "Documento es requerido";
        if (documento.getText().length()>50) return "Documento demasiado largo";
        if (nombre.getText()==null || nombre.getText().trim().isEmpty()) return "Nombre es requerido";
        if (nombre.getText().length()>200) return "Nombre demasiado largo";
        if (tipo.getValue()==null || tipo.getValue().isBlank()) return "Tipo es requerido";
        if (email.getText()!=null && email.getText().length()>0 && !email.getText().contains("@")) return "Email inválido";
        if (email.getText()!=null && email.getText().length()>200) return "Email demasiado largo";
        if (telefono.getText()!=null && telefono.getText().length()>30) return "Teléfono demasiado largo";
        return null;
    }
}
