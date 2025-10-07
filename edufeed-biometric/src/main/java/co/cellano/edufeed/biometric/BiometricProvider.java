package co.cellano.edufeed.biometric;

import java.util.Optional;

public interface BiometricProvider {
    enum Modality {
        FINGERPRINT, FACE, VOICE
    }

    record EnrollmentResult(boolean success, String userId, String detail) {
    }

    record VerificationResult(boolean success, String userId, double score, String detail) {
    }

    EnrollmentResult enroll(String userId, Modality modality);

    VerificationResult verify(Modality modality);

    default Optional<String> getVersion() {
        return Optional.empty();
    }
}
