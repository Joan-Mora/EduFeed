package co.cellano.edufeed.biometric.voice;

import co.cellano.edufeed.biometric.AbstractBiometricProvider;
import co.cellano.edufeed.biometric.BiometricProvider;
import java.util.Optional;

/** Proveedor biométrico de voz: captura audio, extrae rasgos y entrega embedding en Base64. */
public class VoiceRecognitionProvider extends AbstractBiometricProvider implements BiometricProvider {
    private final AudioCaptureService audio;
    private final VoiceFeatureExtractor extractor;
    private final int durationSec;

    public VoiceRecognitionProvider(AudioCaptureService audio, VoiceFeatureExtractor extractor, int durationSec) {
        this.audio = audio;
        this.extractor = extractor;
        this.durationSec = durationSec;
    }

    @Override
    public EnrollmentResult enroll(String userId, Modality modality) {
        if (modality != Modality.VOICE) return new EnrollmentResult(false, userId, "Modalidad no soportada: "+modality);
        if (!audio.isMicrophoneAvailable()) return new EnrollmentResult(false, userId, "Micrófono no disponible");
        Optional<byte[]> pcm = audio.captureSeconds(durationSec);
        if (pcm.isEmpty()) return new EnrollmentResult(false, userId, "No se pudo capturar audio");
        try {
            float[] emb = extractor.extractEmbedding(pcm.get());
            String b64 = VoiceFeatureExtractor.toBase64(emb);
            return new EnrollmentResult(true, userId, b64);
        } catch (Exception e) {
            return new EnrollmentResult(false, userId, "Error extracción de rasgos: "+e.getMessage());
        }
    }

    @Override
    public VerificationResult verify(Modality modality) {
        if (modality != Modality.VOICE) return new VerificationResult(false, null, 0.0, "Modalidad no soportada: "+modality);
        if (!audio.isMicrophoneAvailable()) return new VerificationResult(false, null, 0.0, "Micrófono no disponible");
        Optional<byte[]> pcm = audio.captureSeconds(durationSec);
        if (pcm.isEmpty()) return new VerificationResult(false, null, 0.0, "No se pudo capturar audio");
        try {
            float[] emb = extractor.extractEmbedding(pcm.get());
            String b64 = VoiceFeatureExtractor.toBase64(emb);
            return new VerificationResult(true, null, 0.0, b64);
        } catch (Exception e) {
            return new VerificationResult(false, null, 0.0, "Error extracción de rasgos: "+e.getMessage());
        }
    }
}
