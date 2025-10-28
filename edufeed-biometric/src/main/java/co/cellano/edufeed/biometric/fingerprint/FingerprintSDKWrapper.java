package co.cellano.edufeed.biometric.fingerprint;

import co.cellano.edufeed.biometric.config.BiometricThresholdsConfig;

/**
 * Abstracción del SDK de huella digital (proveedor agnóstico).
 * Implementaciones concretas (DigitalPersona, ZKTeco, Suprema) deberán
 * implementar estos métodos. Incluye una implementación Simulada integrada
 * para desarrollo sin hardware.
 */
public interface FingerprintSDKWrapper {

    /** Inicializa el SDK/canal con el dispositivo. */
    boolean initialize();

    /** Indica si el dispositivo está disponible y listo. */
    boolean isDeviceConnected();

    /** Captura una plantilla de huella (formato binario definido por el SDK). */
    byte[] captureTemplate() throws Exception;

    /**
     * Compara dos plantillas y retorna un score de similitud [0..1].
     * 1.0 es coincidencia perfecta.
     */
    double compareTemplates(byte[] probe, byte[] candidate) throws Exception;

    /** Version del SDK/proveedor. */
    String getSdkVersion();

    /** Configura umbrales/parametrización del SDK. */
    void setThresholds(BiometricThresholdsConfig config);

    /**
     * Implementación simulada sin hardware físico.
     * Útil en entornos de desarrollo/CI.
     */
    class Simulated implements FingerprintSDKWrapper {
        private boolean connected;
        private BiometricThresholdsConfig cfg = new BiometricThresholdsConfig();

        public Simulated(boolean connected) {
            this.connected = connected;
        }

        @Override
        public boolean initialize() {
            // Simula inicialización correcta si se "presenta" hardware
            return connected;
        }

        @Override
        public boolean isDeviceConnected() {
            return connected;
        }

        @Override
        public byte[] captureTemplate() {
            // Plantilla aleatoria fija/determinística en modo simulado
            return ("SIM_FP_TEMPLATE_" + System.currentTimeMillis()).getBytes();
        }

        @Override
        public double compareTemplates(byte[] probe, byte[] candidate) {
            // Score pseudo-aleatorio pero sesgado a acierto alto
            // para facilitar pruebas con umbral >= 0.95
            int mix = (probe.length + candidate.length) % 1000;
            double base = 0.90 + (mix / 1000.0) * 0.1; // [0.90, 1.00)
            return Math.min(0.999, base);
        }

        @Override
        public String getSdkVersion() {
            return "simulated-1.0";
        }

        @Override
        public void setThresholds(BiometricThresholdsConfig config) {
            this.cfg = config;
        }
    }
}
