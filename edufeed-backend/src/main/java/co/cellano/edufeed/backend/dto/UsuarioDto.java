package co.cellano.edufeed.backend.dto;

import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import jakarta.validation.constraints.*;

public class UsuarioDto {
    private String id;

    @NotBlank
    @Size(max = 50)
    private String documento;

    @NotBlank
    @Size(max = 200)
    private String nombreCompleto;

    @NotNull
    private TipoUsuario tipoUsuario;

    @Email
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    private String telefono;

    private Boolean activo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
