package co.cellano.edufeed.backend.dto.response;

import java.time.LocalDate;

/**
 * DTO para reporte de rechazos diarios.
 * Agrupa intentos de acceso denegados por día y motivo.
 * 
 * @since FASE 3.3
 */
public class RechazosDiariosItem {
    private LocalDate dia;
    private String motivoRechazo;
    private Long cantidad;

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }
}
