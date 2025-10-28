package co.cellano.edufeed.desktop.biometric;

import co.cellano.edufeed.biometric.voice.AudioCaptureService;
import co.cellano.edufeed.biometric.voice.AudioCaptureServiceImpl;
import co.cellano.edufeed.biometric.voice.VoiceFeatureExtractor;
import java.util.Optional;

public class LocalBiometricTestService {
    private final String faceSource;
    private final int voiceDurationSec;

    public LocalBiometricTestService(String faceSource, int voiceDurationSec) {
        this.faceSource = faceSource;
        this.voiceDurationSec = voiceDurationSec;
    }

    public String testHardware(String modalidad) {
        try {
            return switch (modalidad.toUpperCase()) {
                case "ROSTRO" -> testFace();
                case "VOZ" -> testVoice();
                case "HUELLA" -> "Lector de huella: demo (integración por SDK específico)";
                default -> "Modalidad no soportada";
            };
        } catch (Exception e) {
            return "Error en prueba local: " + e.getMessage();
        }
    }

    private String testFace() {
        try {
            co.cellano.edufeed.biometric.face.OpenCVFaceDetectorImpl det =
                    new co.cellano.edufeed.biometric.face.OpenCVFaceDetectorImpl(faceSource, 160);
            if (!det.isCameraAvailable() || !det.initialize()) {
                return "Cámara no disponible";
            }
            Optional<byte[]> face = det.captureAlignedFace();
            boolean multi = det.lastFrameHadMultipleFaces();
            if (face.isEmpty()) return "No se detectó rostro";
            if (multi) return "Se detectaron múltiples rostros (mover cámara)";
            return "Rostro detectado y alineado (" + face.get().length + " bytes)";
        } catch (Throwable t) {
            return "OpenCV no disponible: " + t.getMessage();
        }
    }

    private String testVoice() {
        AudioCaptureService audio = new AudioCaptureServiceImpl(16000.0f, 16, 1);
        if (!audio.isMicrophoneAvailable()) return "Micrófono no disponible";
        var pcm = audio.captureSeconds(voiceDurationSec);
        if (pcm.isEmpty()) return "No se pudo capturar audio";
        try {
            float[] emb = new VoiceFeatureExtractor.BasicStats(16000).extractEmbedding(pcm.get());
            return "Audio capturado: embedding[" + emb.length + "] OK";
        } catch (Exception e) {
            return "Error procesando audio: " + e.getMessage();
        }
    }
}
