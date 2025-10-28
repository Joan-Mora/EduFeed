package co.cellano.edufeed.biometric.voice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

/**
 * Extracción de rasgos acústicos. Incluye:
 * - Simulated: vector estable derivado del hash de PCM.
 * - BasicStats: características simples (energía, ZCR) agregadas por frame.
 */
public interface VoiceFeatureExtractor {
    float[] extractEmbedding(byte[] pcm16leMono) throws Exception;

    static String toBase64(float[] emb) {
        ByteBuffer buf = ByteBuffer.allocate(emb.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : emb) buf.putFloat(v);
        return Base64.getEncoder().encodeToString(buf.array());
    }

    static float[] fromBase64(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int n = bytes.length / 4;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) out[i] = buf.getFloat();
        return out;
    }

    class Simulated implements VoiceFeatureExtractor {
        private final int dim;
        public Simulated(int dim) { this.dim = dim; }

        @Override
        public float[] extractEmbedding(byte[] pcm16leMono) throws Exception {
            // Hash-like deterministic mapping to float vector
            int n = dim;
            float[] v = new float[n];
            int acc = 0;
            for (int i = 0; i < pcm16leMono.length; i++) {
                acc = (acc * 31) ^ (pcm16leMono[i] & 0xFF);
                v[i % n] += (acc & 0xFF) / 255.0f;
            }
            // L2 normalize
            float sum = 0f; for (float f: v) sum += f*f; sum = (float)Math.sqrt(sum);
            if (sum > 0) for (int i=0;i<v.length;i++) v[i] /= sum;
            return v;
        }
    }

    class BasicStats implements VoiceFeatureExtractor {
        private final int sampleRate;
        public BasicStats(int sampleRate) { this.sampleRate = sampleRate; }

        @Override
        public float[] extractEmbedding(byte[] pcm16leMono) {
            short[] s = bytesToShorts(pcm16leMono);
            int frameSize = Math.max(1, (int)(0.025 * sampleRate)); // 25ms
            int hop = Math.max(1, (int)(0.010 * sampleRate)); // 10ms
            double meanEnergy=0, meanZCR=0, frames=0, varEnergy=0, varZCR=0;
            for (int start=0; start+frameSize<=s.length; start+=hop) {
                frames++;
                double e=0; int z=0;
                for (int i=0; i<frameSize; i++) {
                    int idx = start+i;
                    int val = s[idx];
                    e += val*val;
                    if (i>0) {
                        if ((s[idx-1] >= 0 && s[idx] < 0) || (s[idx-1] < 0 && s[idx] >= 0)) z++;
                    }
                }
                e /= frameSize; // average energy
                double zcr = (double)z / frameSize;
                // online mean/var
                double deltaE = e - meanEnergy; meanEnergy += deltaE/frames; varEnergy += deltaE*(e-meanEnergy);
                double deltaZ = zcr - meanZCR; meanZCR += deltaZ/frames; varZCR += deltaZ*(zcr-meanZCR);
            }
            double stdEnergy = frames>1 ? Math.sqrt(varEnergy/(frames-1)) : 0;
            double stdZCR = frames>1 ? Math.sqrt(varZCR/(frames-1)) : 0;
            float[] emb = new float[]{(float)meanEnergy,(float)stdEnergy,(float)meanZCR,(float)stdZCR};
            // L2 normalize
            float sum=0; for (float v: emb) sum += v*v; sum=(float)Math.sqrt(sum); if(sum>0) for(int i=0;i<emb.length;i++) emb[i]/=sum;
            return emb;
        }

        private short[] bytesToShorts(byte[] b) {
            int n = b.length/2; short[] s = new short[n];
            for (int i=0;i<n;i++) {
                int lo = b[2*i] & 0xFF; int hi = b[2*i+1] << 8;
                s[i] = (short)(hi | lo);
            }
            return s;
        }
    }
}
