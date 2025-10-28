package co.cellano.edufeed.biometric;

import java.util.Optional;

/**
 * Clase base para proveedores biométricos (POO: herencia y atributos comunes).
 * No altera el contrato existente; las subclases pueden reutilizar manejo de versión.
 */
public abstract class AbstractBiometricProvider implements BiometricProvider {
    protected final String providerName;
    protected final String providerVersion;

    protected AbstractBiometricProvider() {
        this.providerName = null;
        this.providerVersion = null;
    }

    protected AbstractBiometricProvider(String providerName, String providerVersion) {
        this.providerName = providerName;
        this.providerVersion = providerVersion;
    }

    @Override
    public Optional<String> getVersion() {
        if (providerName == null && providerVersion == null) return Optional.empty();
        if (providerName == null) return Optional.ofNullable(providerVersion);
        if (providerVersion == null) return Optional.of(providerName);
        return Optional.of(providerName + "-" + providerVersion);
    }
}
