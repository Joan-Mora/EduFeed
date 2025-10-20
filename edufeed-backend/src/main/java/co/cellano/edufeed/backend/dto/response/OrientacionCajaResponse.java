package co.cellano.edufeed.backend.dto.response;

/**
 * DTO para la orientación a caja cuando se deniega un acceso.
 * Implementa el requisito funcional RF-04.
 */
public class OrientacionCajaResponse {

    private String mensaje;
    private String ubicacionCaja;
    private String horarioAtencion;
    private String referencia;
    private String codigoQR;

    public OrientacionCajaResponse() {
    }

    private OrientacionCajaResponse(Builder builder) {
        this.mensaje = builder.mensaje;
        this.ubicacionCaja = builder.ubicacionCaja;
        this.horarioAtencion = builder.horarioAtencion;
        this.referencia = builder.referencia;
        this.codigoQR = builder.codigoQR;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters y Setters
    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getUbicacionCaja() {
        return ubicacionCaja;
    }

    public void setUbicacionCaja(String ubicacionCaja) {
        this.ubicacionCaja = ubicacionCaja;
    }

    public String getHorarioAtencion() {
        return horarioAtencion;
    }

    public void setHorarioAtencion(String horarioAtencion) {
        this.horarioAtencion = horarioAtencion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getCodigoQR() {
        return codigoQR;
    }

    public void setCodigoQR(String codigoQR) {
        this.codigoQR = codigoQR;
    }

    // Builder pattern
    public static class Builder {
        private String mensaje;
        private String ubicacionCaja;
        private String horarioAtencion;
        private String referencia;
        private String codigoQR;

        public Builder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }

        public Builder ubicacionCaja(String ubicacionCaja) {
            this.ubicacionCaja = ubicacionCaja;
            return this;
        }

        public Builder horarioAtencion(String horarioAtencion) {
            this.horarioAtencion = horarioAtencion;
            return this;
        }

        public Builder referencia(String referencia) {
            this.referencia = referencia;
            return this;
        }

        public Builder codigoQR(String codigoQR) {
            this.codigoQR = codigoQR;
            return this;
        }

        public OrientacionCajaResponse build() {
            return new OrientacionCajaResponse(this);
        }
    }
}
