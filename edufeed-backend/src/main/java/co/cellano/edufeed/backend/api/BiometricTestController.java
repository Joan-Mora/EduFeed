package co.cellano.edufeed.backend.api;

import co.cellano.edufeed.biometric.BiometricProvider;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BiometricTestController {
    private final BiometricProvider provider;

    public BiometricTestController(BiometricProvider provider) {
        this.provider = provider;
    }

    @GetMapping("/biometric/verify/{modality}")
    public Map<String, Object> verify(@PathVariable String modality) {
        var m = BiometricProvider.Modality.valueOf(modality.toUpperCase());
        var res = provider.verify(m);
        return Map.of(
                "success", res.success(),
                "userId", res.userId(),
                "score", res.score(),
                "detail", res.detail()
        );
    }
}
