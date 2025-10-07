package co.cellano.edufeed.biometric;

import java.util.Optional;
import java.util.UUID;

public class MockBiometricProvider implements BiometricProvider {
    @Override
    public EnrollmentResult enroll(String userId, Modality modality) {
        return new EnrollmentResult(true, userId, "MOCK enrolled " + modality);
    }

    @Override
    public VerificationResult verify(Modality modality) {
        return new VerificationResult(true, UUID.randomUUID().toString(), 0.99, "MOCK verified " + modality);
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.of("mock-1.0");
    }
}
