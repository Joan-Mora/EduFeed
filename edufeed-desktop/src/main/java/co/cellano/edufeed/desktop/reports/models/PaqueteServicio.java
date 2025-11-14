package co.cellano.edufeed.desktop.reports.models;

import java.math.BigDecimal;

/**
 * Enum que define los paquetes de servicios disponibles para asignar a
 * usuarios.
 * Cada paquete incluye beneficios, duración y costo.
 */
public enum PaqueteServicio {
    LITE(
            "Paquete Lite",
            "Tiene derecho a recibir almuerzo",
            7,
            new BigDecimal("55000"),
            new String[] { "Almuerzo" }),
    ESTANDAR(
            "Paquete Estándar",
            "Tiene derecho a recibir Desayuno, Almuerzo y Descuento del 20% en productos de la cafetería",
            15,
            new BigDecimal("250000"),
            new String[] { "Desayuno", "Almuerzo", "Descuento 20% en cafetería" }),
    PREMIUM(
            "Paquete Premium",
            "Tiene derecho a desayuno, almuerzo, cena y descuentos del 40% en productos de cafetería",
            30,
            new BigDecimal("600000"),
            new String[] { "Desayuno", "Almuerzo", "Cena", "Descuento 40% en cafetería" });

    private final String nombre;
    private final String descripcion;
    private final int duracionDias;
    private final BigDecimal costo;
    private final String[] beneficios;

    PaqueteServicio(String nombre, String descripcion, int duracionDias, BigDecimal costo, String[] beneficios) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionDias = duracionDias;
        this.costo = costo;
        this.beneficios = beneficios;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getDuracionDias() {
        return duracionDias;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public String[] getBeneficios() {
        return beneficios;
    }

    /**
     * Genera el motivo de pago que se usará al crear el pago.
     * Formato: "Compra - Paquete [Nombre]"
     */
    public String getMotivoPago() {
        return "Compra - " + nombre;
    }

    /**
     * Retorna el nombre del paquete para mostrar en UI (sin "Paquete" prefijo).
     */
    public String getNombreCorto() {
        return nombre.replace("Paquete ", "");
    }

    @Override
    public String toString() {
        return nombre + " - $" + costo + " (" + duracionDias + " días)";
    }
}
