package co.cellano.edufeed.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item de respuesta para ingresos diarios provenientes de mv_ingresos_diarios.
 */
public class IngresosDiariosItem {
    private LocalDate dia;
    private String tipoPago;
    private String metodoPago;
    private Long cantidad;
    private BigDecimal total;

    public IngresosDiariosItem() {
    }

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
