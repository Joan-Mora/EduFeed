package co.cellano.edufeed.backend.config;

import co.cellano.edufeed.biometric.BiometricProvider;
import co.cellano.edufeed.biometric.CompositeBiometricProvider;
import co.cellano.edufeed.biometric.MockBiometricProvider;
import co.cellano.edufeed.biometric.config.BiometricThresholdsConfig;
import co.cellano.edufeed.biometric.face.FaceNetEmbeddingExtractor;
import co.cellano.edufeed.biometric.face.FaceRecognitionProvider;
import co.cellano.edufeed.biometric.face.OpenCVFaceDetector;
import co.cellano.edufeed.biometric.fingerprint.FingerprintSDKWrapper;
import co.cellano.edufeed.biometric.fingerprint.HardwareFingerprintProvider;
import co.cellano.edufeed.biometric.fingerprint.vendor.DigitalPersonaFingerprintWrapper;
import co.cellano.edufeed.biometric.fingerprint.vendor.SupremaFingerprintWrapper;
import co.cellano.edufeed.biometric.fingerprint.vendor.ZKTecoFingerprintWrapper;
import co.cellano.edufeed.biometric.voice.AudioCaptureService;
import co.cellano.edufeed.biometric.voice.AudioCaptureServiceImpl;
import co.cellano.edufeed.biometric.voice.VoiceFeatureExtractor;
import co.cellano.edufeed.biometric.voice.VoiceRecognitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class BiometricConfig {

    private static final Logger log = LoggerFactory.getLogger(BiometricConfig.class);

    @Bean
    public BiometricProvider biometricProvider(Environment env) {
        String provider = env.getProperty("edufeed.biometric.provider", "mock");

        // Umbrales configurables con valores por defecto alineados al criterio de aceptación
        double far = env.getProperty("edufeed.biometric.far", Double.class, 0.0001);
        double frr = env.getProperty("edufeed.biometric.frr", Double.class, 0.05);
        double matchThreshold = env.getProperty("edufeed.biometric.match-threshold", Double.class, 0.95);
        BiometricThresholdsConfig thresholds = new BiometricThresholdsConfig(far, frr, matchThreshold);

        // Simulación opcional del hardware (útil para dev/CI)
        boolean simulate = env.getProperty("edufeed.biometric.simulateHardware", Boolean.class, false)
                || Boolean.parseBoolean(System.getenv().getOrDefault("EDUFEED_HARDWARE_PRESENT", "false"));

        BiometricProvider fingerprintProvider = null;
        if ("hardware".equalsIgnoreCase(provider)) {
            String vendor = env.getProperty("edufeed.biometric.vendor", "simulated");
            FingerprintSDKWrapper wrapper;
            switch (vendor.toLowerCase()) {
                case "digitalpersona" -> wrapper = new DigitalPersonaFingerprintWrapper();
                case "zkteco" -> wrapper = new ZKTecoFingerprintWrapper();
                case "suprema" -> wrapper = new SupremaFingerprintWrapper();
                case "simulated" -> wrapper = new FingerprintSDKWrapper.Simulated(simulate);
                default -> {
                    log.warn("Vendor '{}' no reconocido. Usando modo simulado.", vendor);
                    wrapper = new FingerprintSDKWrapper.Simulated(simulate);
                }
            }
            if (wrapper.isDeviceConnected() && wrapper.initialize()) {
                log.info("Usando HardwareFingerprintProvider (SDK version: {}) con thresholds {}",
                        wrapper.getSdkVersion(), thresholds);
                fingerprintProvider = new HardwareFingerprintProvider(wrapper, thresholds);
            } else {
                log.warn("Proveedor 'hardware' seleccionado pero el dispositivo no está disponible o no inicializó (vendor={}). Fallback a MOCK.", vendor);
            }
        }
        if (fingerprintProvider == null) {
            fingerprintProvider = new MockBiometricProvider();
        }

        // Configurar proveedor facial (simulado por defecto). Modelo ONNX opcional.
    int faceDim = env.getProperty("edufeed.biometric.face.dim", Integer.class, 128);
    boolean faceSimulate = env.getProperty("edufeed.biometric.face.simulate", Boolean.class, true);
    String faceSource = env.getProperty("edufeed.biometric.face.source", "camera:0");
    OpenCVFaceDetector faceDetector = faceSimulate
        ? new OpenCVFaceDetector.Simulated(true)
        : new co.cellano.edufeed.biometric.face.OpenCVFaceDetectorImpl(faceSource, 160);
        FaceNetEmbeddingExtractor faceExtractor = new FaceNetEmbeddingExtractor.Simulated(faceDim);
    FaceRecognitionProvider faceProvider = new FaceRecognitionProvider(faceDetector, faceExtractor, faceDim);

    // Proveedor de voz (simulado o básico con micrófono)
    boolean voiceSimulate = env.getProperty("edufeed.biometric.voice.simulate", Boolean.class, true);
    int voiceSeconds = env.getProperty("edufeed.biometric.voice.duration", Integer.class, 4);
    int voiceSampleRate = env.getProperty("edufeed.biometric.voice.sample-rate", Integer.class, 16000);
    AudioCaptureService audioService = new AudioCaptureServiceImpl(voiceSampleRate, 16, 1);
    VoiceFeatureExtractor voiceExtractor = voiceSimulate
        ? new VoiceFeatureExtractor.Simulated(16)
        : new VoiceFeatureExtractor.BasicStats(voiceSampleRate);
    VoiceRecognitionProvider voiceProvider = new VoiceRecognitionProvider(audioService, voiceExtractor, voiceSeconds);

        // Componer proveedor por modalidad
        CompositeBiometricProvider composite = new CompositeBiometricProvider()
                .with(BiometricProvider.Modality.FINGERPRINT, fingerprintProvider)
        .with(BiometricProvider.Modality.FACE, faceProvider)
        .with(BiometricProvider.Modality.VOICE, voiceProvider);

    log.info("BiometricProvider compuesto inicializado (fingerprint={}, face={}, voice={})",
        fingerprintProvider.getClass().getSimpleName(),
        faceSimulate ? "simulado" : "opencv",
        voiceSimulate ? "simulado" : "basic-stats");
        return composite;
    }
}
