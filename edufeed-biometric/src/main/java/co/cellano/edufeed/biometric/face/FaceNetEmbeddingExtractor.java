package co.cellano.edufeed.biometric.face;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

/**
 * Extractor de embeddings tipo FaceNet (placeholder con modo simulado).
 */
public interface FaceNetEmbeddingExtractor {
    /** Carga el modelo ONNX si está disponible. */
    boolean loadModel(Optional<byte[]> onnxBytes);

    /**
     * Extrae un embedding unit-normalizado (p.ej. 128D/512D) de la imagen de rostro.
     */
    float[] extractEmbedding(byte[] alignedFaceImageBytes) throws Exception;

    /** Serializa a Base64 (little-endian) */
    static String toBase64(float[] emb) {
        ByteBuffer buf = ByteBuffer.allocate(emb.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : emb) buf.putFloat(v);
        return Base64.getEncoder().encodeToString(buf.array());
    }

    /** Deserializa desde Base64 (little-endian) */
    static float[] fromBase64(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int n = bytes.length / 4;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) out[i] = buf.getFloat();
        return out;
    }

    /** Implementación simulada basada en hash para estabilidad entre capturas similares. */
    class Simulated implements FaceNetEmbeddingExtractor {
        private final int dim;

        public Simulated(int dim) { this.dim = dim; }

        @Override
        public boolean loadModel(Optional<byte[]> onnxBytes) { return true; }

        @Override
        public float[] extractEmbedding(byte[] alignedFaceImageBytes) throws Exception {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(alignedFaceImageBytes);
            float[] emb = new float[dim];
            for (int i = 0; i < dim; i++) {
                int b = h[i % h.length] & 0xFF;
                emb[i] = (b / 255.0f); // [0,1]
            }
            // normalizar a norma 1
            float sum = 0f; for (float v: emb) sum += v*v; sum = (float)Math.sqrt(sum);
            if (sum > 0) for (int i=0;i<emb.length;i++) emb[i] /= sum;
            return emb;
        }
    }
}
