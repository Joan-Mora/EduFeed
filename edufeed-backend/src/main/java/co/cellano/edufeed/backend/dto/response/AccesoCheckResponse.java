package co.cellano.edufeed.backend.dto.response;

import co.cellano.edufeed.backend.dto.DerechoUsoDto;
import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.model.enums.Modalidad;

import java.time.OffsetDateTime;

/**
 * DTO para la respuesta de verificación de acceso.
 * Contiene el resultado de la verificación (permitido/denegado) y
 * la información relevante según el caso.
 */
public class AccesoCheckResponse {

    private Boolean permitido;
    private UsuarioDto usuario;
    private DerechoUsoDto derecho;
    private String motivo;
    private Modalidad modalidad;
    private OrientacionCajaResponse orientacionCaja;
    private OffsetDateTime timestamp;

    public AccesoCheckResponse() {
    }

    private AccesoCheckResponse(Builder builder) {
        this.permitido = builder.permitido;
        this.usuario = builder.usuario;
        this.derecho = builder.derecho;
        this.motivo = builder.motivo;
        this.modalidad = builder.modalidad;
        this.orientacionCaja = builder.orientacionCaja;
        this.timestamp = builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters y Setters
    public Boolean getPermitido() {
        return permitido;
    }

    public void setPermitido(Boolean permitido) {
        this.permitido = permitido;
    }

    public UsuarioDto getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDto usuario) {
        this.usuario = usuario;
    }

    public DerechoUsoDto getDerecho() {
        return derecho;
    }

    public void setDerecho(DerechoUsoDto derecho) {
        this.derecho = derecho;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public void setModalidad(Modalidad modalidad) {
        this.modalidad = modalidad;
    }

    public OrientacionCajaResponse getOrientacionCaja() {
        return orientacionCaja;
    }

    public void setOrientacionCaja(OrientacionCajaResponse orientacionCaja) {
        this.orientacionCaja = orientacionCaja;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Builder pattern
    public static class Builder {
        private Boolean permitido;
        private UsuarioDto usuario;
        private DerechoUsoDto derecho;
        private String motivo;
        private Modalidad modalidad;
        private OrientacionCajaResponse orientacionCaja;
        private OffsetDateTime timestamp;

        public Builder permitido(Boolean permitido) {
            this.permitido = permitido;
            return this;
        }

        public Builder usuario(UsuarioDto usuario) {
            this.usuario = usuario;
            return this;
        }

        public Builder derecho(DerechoUsoDto derecho) {
            this.derecho = derecho;
            return this;
        }

        public Builder motivo(String motivo) {
            this.motivo = motivo;
            return this;
        }

        public Builder modalidad(Modalidad modalidad) {
            this.modalidad = modalidad;
            return this;
        }

        public Builder orientacionCaja(OrientacionCajaResponse orientacionCaja) {
            this.orientacionCaja = orientacionCaja;
            return this;
        }

        public Builder timestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AccesoCheckResponse build() {
            return new AccesoCheckResponse(this);
        }
    }
}
