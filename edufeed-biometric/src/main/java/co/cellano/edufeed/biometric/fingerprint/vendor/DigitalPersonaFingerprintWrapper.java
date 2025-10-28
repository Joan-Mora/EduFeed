package co.cellano.edufeed.biometric.fingerprint.vendor;

import co.cellano.edufeed.biometric.config.BiometricThresholdsConfig;
import co.cellano.edufeed.biometric.fingerprint.FingerprintSDKWrapper;

/**
 * Wrapper para SDK DigitalPersona (placeholder).
 * Reemplace los métodos con llamadas reales al SDK del fabricante.
 */
public class DigitalPersonaFingerprintWrapper implements FingerprintSDKWrapper {
    private BiometricThresholdsConfig cfg = new BiometricThresholdsConfig();

    @Override
    public boolean initialize() {
        // TODO: Inicializar SDK DigitalPersona
        return false; // por defecto no disponible hasta implementar
    }

    @Override
    public boolean isDeviceConnected() {
        // TODO: detectar dispositivo
        return false;
    }

    @Override
    public byte[] captureTemplate() throws Exception {
        throw new UnsupportedOperationException("DigitalPersona SDK no implementado");
    }

    @Override
    public double compareTemplates(byte[] probe, byte[] candidate) throws Exception {
        throw new UnsupportedOperationException("DigitalPersona SDK no implementado");
    }

    @Override
    public String getSdkVersion() {
        return "digitalpersona-unimplemented";
    }

    @Override
    public void setThresholds(BiometricThresholdsConfig config) {
        this.cfg = config;
    }
}
