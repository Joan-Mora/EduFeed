package co.cellano.edufeed.biometric.voice;

import java.util.Optional;

/** Captura de audio desde micrófono (PCM 16kHz mono 16-bit LE por defecto). */
public interface AudioCaptureService {
    /** @return true si hay un micrófono disponible. */
    boolean isMicrophoneAvailable();

    /** Captura una muestra de duración fija (segundos) y retorna bytes PCM 16-bit mono. */
    Optional<byte[]> captureSeconds(int seconds);
}
