package co.cellano.edufeed.biometric.voice;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import javax.sound.sampled.*;

/** Implementación con Java Sound API. */
public class AudioCaptureServiceImpl implements AudioCaptureService {
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;

    public AudioCaptureServiceImpl(float sampleRate, int sampleSizeInBits, int channels) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
    }

    private AudioFormat format() {
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false);
    }

    @Override
    public boolean isMicrophoneAvailable() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format());
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<byte[]> captureSeconds(int seconds) {
        if (seconds <= 0) seconds = 3;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format());
            if (!AudioSystem.isLineSupported(info)) return Optional.empty();
            try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
                line.open(format());
                line.start();
                int bufferSize = (int) (format().getFrameRate() * format().getFrameSize() / 10); // 100ms
                byte[] buffer = new byte[bufferSize];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                long endTime = System.currentTimeMillis() + seconds * 1000L;
                while (System.currentTimeMillis() < endTime) {
                    int n = line.read(buffer, 0, buffer.length);
                    if (n > 0) out.write(buffer, 0, n);
                }
                line.stop();
                return Optional.of(out.toByteArray());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
