package co.cellano.edufeed.biometric.face;

import java.util.Optional;

/**
 * Detector de rostro. Implementación por defecto simulada para desarrollo sin cámara.
 */
public interface OpenCVFaceDetector {
    boolean initialize();

    /**
     * @return true si la cámara/entorno está disponible.
     */
    boolean isCameraAvailable();

    /**
     * Captura y devuelve una imagen de rostro recortada y alineada (bytes, p.ej. PNG/JPG).
     */
    Optional<byte[]> captureAlignedFace();

    /**
     * Devuelve true si el último frame tenía más de un rostro (para rechazo explícito).
     */
    boolean lastFrameHadMultipleFaces();

    /** Implementación simulada. */
    class Simulated implements OpenCVFaceDetector {
        private final boolean available;
        private boolean multi = false;

        public Simulated(boolean available) {
            this.available = available;
        }

        @Override
        public boolean initialize() { return available; }

        @Override
        public boolean isCameraAvailable() { return available; }

        @Override
        public Optional<byte[]> captureAlignedFace() {
            // Devuelve bytes determinísticos estilo imagen sintética
            this.multi = false; // por defecto un solo rostro
            String content = "SIM_FACE_" + System.currentTimeMillis();
            return available ? Optional.of(content.getBytes()) : Optional.empty();
        }

        @Override
        public boolean lastFrameHadMultipleFaces() { return multi; }
    }
}
