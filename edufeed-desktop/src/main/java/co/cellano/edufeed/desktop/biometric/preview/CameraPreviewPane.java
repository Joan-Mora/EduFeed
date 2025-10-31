package co.cellano.edufeed.desktop.biometric.preview;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

/** Vista previa de cámara usando OpenCV (source: "camera:0" o URL rtsp/http). */
public class CameraPreviewPane extends BorderPane implements PreviewControl {
    private final String source;
    private final ImageView image = new ImageView();
    private volatile boolean running = false;
    private VideoCapture capture;
    private ScheduledExecutorService executor;

    public CameraPreviewPane(String source) {
        this.source = source == null || source.isBlank() ? "camera:0" : source;
        setPadding(new Insets(8));
        image.setPreserveRatio(true);
        image.setFitWidth(360);
        image.setFitHeight(220);
        setCenter(image);
    }

    @Override
    public void start() {
        if (running) return;
        try {
            OpenCV.loadLocally();
        } catch (Throwable ignore) {}
        capture = new VideoCapture();
        if (source.startsWith("camera:")) {
            int idx = 0;
            try { idx = Integer.parseInt(source.substring("camera:".length())); } catch (Exception ignore) {}
            capture.open(idx);
        } else {
            capture.open(source);
        }
        if (!capture.isOpened()) {
            setPlaceholder("Cámara no disponible");
            return;
        }
        running = true;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "camera-preview");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::grabAndShow, 0, 66, TimeUnit.MILLISECONDS); // ~15fps
    }

    private void grabAndShow() {
        if (!running || capture == null) return;
        Mat frame = new Mat();
        if (!capture.read(frame) || frame.empty()) return;
        MatOfByte mob = new MatOfByte();
        if (!Imgcodecs.imencode(".bmp", frame, mob)) return;
        byte[] bytes = mob.toArray();
        Platform.runLater(() -> image.setImage(new Image(new ByteArrayInputStream(bytes))));
    }

    private void setPlaceholder(String text) {
        Platform.runLater(() -> {
            image.setImage(null);
            image.setUserData(text);
        });
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (capture != null) {
            try { capture.release(); } catch (Throwable ignore) {}
            capture = null;
        }
    }
}
