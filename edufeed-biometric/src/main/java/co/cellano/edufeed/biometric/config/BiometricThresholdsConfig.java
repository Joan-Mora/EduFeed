package co.cellano.edufeed.biometric.config;

/**
 * Configuración de umbrales biométricos.
 * FAR (False Acceptance Rate) y FRR (False Rejection Rate) se expresan en rango [0,1].
 * matchThreshold es el umbral de similitud para considerar una coincidencia (p.ej. 0.95).
 */
public class BiometricThresholdsConfig {
    private double far; // p.ej. 0.0001 -> 0.01%
    private double frr; // p.ej. 0.05 -> 5%
    private double matchThreshold; // p.ej. 0.95 -> 95%

    public BiometricThresholdsConfig() {
        // Valores por defecto alineados al criterio de aceptación
        this(0.0001, 0.05, 0.95);
    }

    public BiometricThresholdsConfig(double far, double frr, double matchThreshold) {
        this.far = far;
        this.frr = frr;
        this.matchThreshold = matchThreshold;
    }

    public double getFar() {
        return far;
    }

    public void setFar(double far) {
        this.far = far;
    }

    public double getFrr() {
        return frr;
    }

    public void setFrr(double frr) {
        this.frr = frr;
    }

    public double getMatchThreshold() {
        return matchThreshold;
    }

    public void setMatchThreshold(double matchThreshold) {
        this.matchThreshold = matchThreshold;
    }

    @Override
    public String toString() {
        return "BiometricThresholdsConfig{" +
                "far=" + far +
                ", frr=" + frr +
                ", matchThreshold=" + matchThreshold +
                '}';
    }
}
