package co.cellano.edufeed.desktop.biometric.preview;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

/** Vista previa con detector real (Haar cascade) sobre el stream de cámara. */
public class FaceDetectorPreviewPane extends BorderPane implements PreviewControl {
    private final String source;
    private final String cascadePathOverride;
    private final double scaleFactor;
    private final int minNeighbors;
    private final int minSize;
    private final ImageView image = new ImageView();
    private final Text status = new Text("Detector: inicializando…");
    private volatile boolean running = false;
    private VideoCapture capture;
    private ScheduledExecutorService executor;
    private CascadeClassifier faceCascade;

    public FaceDetectorPreviewPane(String source, double scaleFactor, int minNeighbors, int minSize) {
        this(source, scaleFactor, minNeighbors, minSize, null);
    }

    public FaceDetectorPreviewPane(String source, double scaleFactor, int minNeighbors, int minSize,
            String cascadePath) {
        this.source = (source == null || source.isBlank()) ? "camera:0" : source;
        this.cascadePathOverride = cascadePath;
        this.scaleFactor = (scaleFactor <= 1.0 ? 1.1 : scaleFactor);
        this.minNeighbors = Math.max(1, minNeighbors);
        this.minSize = Math.max(20, minSize);
        setPadding(new Insets(8));
        image.setPreserveRatio(true);
        image.setFitWidth(360);
        image.setFitHeight(220);
        setTop(status);
        setCenter(image);
    }

    @Override
    public void start() {
        if (running)
            return;
        try {
            OpenCV.loadLocally();
        } catch (Throwable ignore) {
        }
        // Cargar cascade: primero desde classpath, si no existe, usar variable de
        // entorno DESKTOP_FACE_CASCADE
        try {
            Path cascadePath = null;
            if (cascadePathOverride != null && !cascadePathOverride.isBlank()) {
                cascadePath = Path.of(cascadePathOverride);
            } else {
                cascadePath = tryExport("/haarcascades/haarcascade_frontalface_default.xml");
                if (cascadePath == null) {
                    String envPath = System.getenv("DESKTOP_FACE_CASCADE");
                    if (envPath != null && !envPath.isBlank())
                        cascadePath = Path.of(envPath);
                }
            }
            if (cascadePath != null && Files.exists(cascadePath)) {
                faceCascade = new CascadeClassifier(cascadePath.toString());
            }
        } catch (Exception ignore) {
        }

        if (faceCascade == null || faceCascade.empty()) {
            setStatus("Cascade no encontrado: sin detección (configure DESKTOP_FACE_CASCADE)");
        } else {
            setStatus("Detector OK");
        }

        capture = new VideoCapture();
        if (source.startsWith("camera:")) {
            int idx = 0;
            try {
                idx = Integer.parseInt(source.substring("camera:".length()));
            } catch (Exception ignore) {
            }
            capture.open(idx);
        } else {
            capture.open(source);
        }
        if (!capture.isOpened()) {
            setStatus("Cámara no disponible");
            return;
        }
        running = true;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "face-detector-preview");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::grabDetectAndShow, 0, 66, TimeUnit.MILLISECONDS);
    }

    private void grabDetectAndShow() {
        if (!running || capture == null)
            return;
        Mat frame = new Mat();
        if (!capture.read(frame) || frame.empty())
            return;

        if (faceCascade != null && !faceCascade.empty()) {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);
            MatOfRect faces = new MatOfRect();
            faceCascade.detectMultiScale(gray, faces, scaleFactor, minNeighbors, 0, new Size(minSize, minSize),
                    new Size());
            for (Rect r : faces.toArray()) {
                Imgproc.rectangle(frame, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height),
                        new Scalar(0, 255, 0), 2);
            }
        }

        MatOfByte mob = new MatOfByte();
        if (!Imgcodecs.imencode(".bmp", frame, mob))
            return;
        byte[] bytes = mob.toArray();
        Platform.runLater(() -> image.setImage(new Image(new ByteArrayInputStream(bytes))));
    }

    private Path tryExport(String resource) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            if (is == null)
                return null;
            Path tmp = Files.createTempFile("cascade", ".xml");
            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return tmp;
        }
    }

    private void setStatus(String s) {
        Platform.runLater(() -> status.setText(s));
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (capture != null) {
            try {
                capture.release();
            } catch (Throwable ignore) {
            }
            capture = null;
        }
    }
}
