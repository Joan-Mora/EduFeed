package co.cellano.edufeed.biometric.fingerprint.vendor;

import co.cellano.edufeed.biometric.config.BiometricThresholdsConfig;
import co.cellano.edufeed.biometric.fingerprint.FingerprintSDKWrapper;

/**
 * Wrapper para SDK Suprema (placeholder).
 */
public class SupremaFingerprintWrapper implements FingerprintSDKWrapper {
    private BiometricThresholdsConfig cfg = new BiometricThresholdsConfig();

    @Override
    public boolean initialize() { return false; }

    @Override
    public boolean isDeviceConnected() { return false; }

    @Override
    public byte[] captureTemplate() { throw new UnsupportedOperationException("Suprema SDK no implementado"); }

    @Override
    public double compareTemplates(byte[] probe, byte[] candidate) { throw new UnsupportedOperationException("Suprema SDK no implementado"); }

    @Override
    public String getSdkVersion() { return "suprema-unimplemented"; }

    @Override
    public void setThresholds(BiometricThresholdsConfig config) { this.cfg = config; }
}
