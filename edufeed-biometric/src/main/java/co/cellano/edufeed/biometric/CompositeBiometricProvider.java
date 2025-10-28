package co.cellano.edufeed.biometric;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Proveedor compuesto que delega por modalidad al proveedor específico.
 * Si una modalidad no tiene proveedor asignado, usa un MockBiometricProvider.
 */
public class CompositeBiometricProvider extends AbstractBiometricProvider implements BiometricProvider {

    private final Map<Modality, BiometricProvider> providers = new EnumMap<>(Modality.class);
    private final BiometricProvider fallback = new MockBiometricProvider();

    public CompositeBiometricProvider with(Modality modality, BiometricProvider provider) {
        if (provider != null) {
            providers.put(modality, provider);
        }
        return this;
    }

    private BiometricProvider pick(Modality modality) {
        return providers.getOrDefault(modality, fallback);
    }

    @Override
    public EnrollmentResult enroll(String userId, Modality modality) {
        return pick(modality).enroll(userId, modality);
    }

    @Override
    public VerificationResult verify(Modality modality) {
        return pick(modality).verify(modality);
    }

    @Override
    public Optional<String> getVersion() {
        // Concatena versiones conocidas
        StringBuilder sb = new StringBuilder();
        for (var e : providers.entrySet()) {
            e.getValue().getVersion().ifPresent(v -> {
                if (!sb.isEmpty()) sb.append("; ");
                sb.append(e.getKey().name()).append(":").append(v);
            });
        }
        return sb.isEmpty() ? Optional.empty() : Optional.of(sb.toString());
    }
}
