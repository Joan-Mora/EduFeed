package co.cellano.edufeed.biometric.face;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

/**
 * Implementación real con OpenCV: captura desde cámara (laptop/USB) o stream IP y detecta rostro con Haar cascade.
 * - source: "camera:0" para webcam integrada/USB, o una URL (http/rtsp) para cámara de móvil (IP Webcam).
 */
public class OpenCVFaceDetectorImpl implements OpenCVFaceDetector {
    private final String source;
    private final int faceSize;
    private VideoCapture capture;
    private CascadeClassifier faceCascade;
    private volatile boolean multiFaces = false;

    public OpenCVFaceDetectorImpl(String source, int faceSize) {
        this.source = source;
        this.faceSize = faceSize;
    }

    @Override
    public boolean initialize() {
        try {
            // Cargar librería nativa
            OpenCV.loadLocally();

            // Cargar cascade desde recursos a archivo temporal
            Path tmpCascade = exportResource("/haarcascades/haarcascade_frontalface_default.xml");
            if (tmpCascade == null) return false;
            faceCascade = new CascadeClassifier(tmpCascade.toString());
            if (faceCascade.empty()) return false;

            // Abrir captura
            capture = new VideoCapture();
            if (source != null && source.startsWith("camera:")) {
                int idx = 0;
                try { idx = Integer.parseInt(source.substring("camera:".length())); } catch (Exception ignore) {}
                capture.open(idx);
            } else {
                String url = (source == null || source.isBlank()) ? "" : source;
                capture.open(url);
            }
            return capture.isOpened();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isCameraAvailable() {
        return capture != null && capture.isOpened();
    }

    @Override
    public Optional<byte[]> captureAlignedFace() {
        if (!isCameraAvailable()) return Optional.empty();

        Mat frame = new Mat();
        if (!capture.read(frame) || frame.empty()) return Optional.empty();

        // Detectar
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        Rect[] facesArray = detectFaces(gray);
        multiFaces = facesArray.length > 1;
        if (facesArray.length != 1) {
            return Optional.empty();
        }

        // Recortar y redimensionar
        Rect face = facesArray[0];
        Rect bounded = boundRect(face, frame.width(), frame.height());
        Mat roi = new Mat(frame, bounded);
        Mat resized = new Mat();
        Imgproc.resize(roi, resized, new Size(faceSize, faceSize));

        // Codificar PNG
        MatOfByte mob = new MatOfByte();
        if (!Imgcodecs.imencode(".png", resized, mob)) {
            return Optional.empty();
        }
        return Optional.of(mob.toArray());
    }

    @Override
    public boolean lastFrameHadMultipleFaces() {
        return multiFaces;
    }

    private Rect[] detectFaces(Mat gray) {
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(60, 60), new Size());
        return faces.toArray();
    }

    private Rect boundRect(Rect r, int w, int h) {
        int x = Math.max(0, r.x);
        int y = Math.max(0, r.y);
        int xe = Math.min(w, r.x + r.width);
        int ye = Math.min(h, r.y + r.height);
        return new Rect(x, y, Math.max(1, xe - x), Math.max(1, ye - y));
    }

    private Path exportResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            Path tmp = Files.createTempFile("haarcascade", ".xml");
            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return tmp;
        }
    }
}
