package co.cellano.edufeed.biometric.face;

import co.cellano.edufeed.biometric.AbstractBiometricProvider;
import co.cellano.edufeed.biometric.BiometricProvider;
import java.util.Optional;

/**
 * Proveedor de reconocimiento facial apoyado en detector OpenCV y extractor
 * FaceNet.
 * Contrato:
 * - enroll(FACE): retorna detail con embedding en Base64 (float32,
 * little-endian).
 * - verify(FACE): retorna detail con embedding en Base64 para que el servicio
 * compare.
 */
@SuppressWarnings("unused")
public class FaceRecognitionProvider extends AbstractBiometricProvider implements BiometricProvider {

    private final OpenCVFaceDetector detector;
    private final FaceNetEmbeddingExtractor extractor;
    private final int embeddingDim;

    public FaceRecognitionProvider(OpenCVFaceDetector detector, FaceNetEmbeddingExtractor extractor, int embeddingDim) {
        this.detector = detector;
        this.extractor = extractor;
        this.embeddingDim = embeddingDim;
    }

    @Override
    public EnrollmentResult enroll(String userId, Modality modality) {
        if (modality != Modality.FACE) {
            return new EnrollmentResult(false, userId, "Modalidad no soportada: " + modality);
        }
        if (!detector.isCameraAvailable() || !detector.initialize()) {
            return new EnrollmentResult(false, userId, "Cámara no disponible");
        }
        try {
            Optional<byte[]> faceBytesOpt = detector.captureAlignedFace();
            if (faceBytesOpt.isEmpty()) {
                return new EnrollmentResult(false, userId, "No se detectó rostro");
            }
            if (detector.lastFrameHadMultipleFaces()) {
                return new EnrollmentResult(false, userId, "Se detectaron múltiples rostros");
            }
            float[] emb = extractor.extractEmbedding(faceBytesOpt.get());
            if (emb.length != embeddingDim) {
                return new EnrollmentResult(false, userId, "Dimensión de embedding inesperada");
            }
            String b64 = FaceNetEmbeddingExtractor.toBase64(emb);
            return new EnrollmentResult(true, userId, b64);
        } catch (Exception e) {
            return new EnrollmentResult(false, userId, "Error captura/embedding: " + e.getMessage());
        }
    }

    @Override
    public VerificationResult verify(Modality modality) {
        if (modality != Modality.FACE) {
            return new VerificationResult(false, null, 0.0, "Modalidad no soportada: " + modality);
        }
        if (!detector.isCameraAvailable() || !detector.initialize()) {
            return new VerificationResult(false, null, 0.0, "Cámara no disponible");
        }
        try {
            Optional<byte[]> faceBytesOpt = detector.captureAlignedFace();
            if (faceBytesOpt.isEmpty()) {
                return new VerificationResult(false, null, 0.0, "No se detectó rostro");
            }
            if (detector.lastFrameHadMultipleFaces()) {
                return new VerificationResult(false, null, 0.0, "Se detectaron múltiples rostros");
            }
            float[] emb = extractor.extractEmbedding(faceBytesOpt.get());
            if (emb.length != embeddingDim) {
                return new VerificationResult(false, null, 0.0, "Dimensión de embedding inesperada");
            }
            String b64 = FaceNetEmbeddingExtractor.toBase64(emb);
            // score=0.0 porque la comparación se realiza en el servicio vía cosine
            // similarity
            return new VerificationResult(true, null, 0.0, b64);
        } catch (Exception e) {
            return new VerificationResult(false, null, 0.0, "Error captura/embedding: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.of("face-simulated");
    }
}
