package co.cellano.edufeed.desktop.biometric.preview;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/** Vista previa en vivo del micrófono dibujando una forma de onda simple. */
public class VoiceWaveformPane extends BorderPane implements PreviewControl {
    private final float sampleRate = 16000f;
    private final int sampleSizeInBits = 16;
    private final int channels = 1;
    private final Canvas canvas = new Canvas(360, 120);
    private ScheduledExecutorService executor;
    private TargetDataLine line;
    private volatile boolean running = false;

    public VoiceWaveformPane() {
        setPadding(new Insets(8));
        setCenter(canvas);
    }

    private AudioFormat fmt() { return new AudioFormat(sampleRate, sampleSizeInBits, channels, true, false); }

    @Override
    public void start() {
        if (running) return;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt());
            if (!AudioSystem.isLineSupported(info)) {
                drawMessage("Micrófono no disponible");
                return;
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(fmt());
            line.start();
            running = true;
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "voice-preview");
                t.setDaemon(true);
                return t;
            });
            executor.scheduleAtFixedRate(this::readAndDraw, 0, 50, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            drawMessage("Error micrófono: " + e.getMessage());
        }
    }

    private void readAndDraw() {
        if (!running || line == null) return;
        byte[] buf = new byte[1024];
        int n = line.read(buf, 0, buf.length);
        if (n <= 0) return;
        // Convertir a muestras de 16-bit LE
        int samples = n / 2;
        float[] vals = new float[samples];
        for (int i = 0; i < samples; i++) {
            int lo = buf[i*2] & 0xFF;
            int hi = buf[i*2+1];
            int s = (hi << 8) | lo;
            vals[i] = s / 32768f;
        }
        Platform.runLater(() -> drawWave(vals));
    }

    private void drawWave(float[] vals) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.web("#f8f9fa"));
        g.fillRect(0, 0, w, h);
        g.setStroke(Color.web("#007bff"));
        g.setLineWidth(1.5);
        double mid = h / 2;
        int step = Math.max(1, vals.length / (int)w);
        double x = 0;
        for (int i = 0; i < vals.length; i += step) {
            double y = mid - (vals[i] * (h/2 - 2));
            if (i == 0) g.beginPath();
            else g.lineTo(x, y);
            if (i == 0) g.moveTo(x, y);
            x += 1;
        }
        g.stroke();
    }

    private void drawMessage(String msg) {
        Platform.runLater(() -> {
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.web("#f8f9fa"));
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.setFill(Color.web("#6c757d"));
            g.fillText(msg, 10, canvas.getHeight()/2);
        });
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (line != null) {
            try { line.stop(); line.close(); } catch (Exception ignore) {}
            line = null;
        }
    }
}
