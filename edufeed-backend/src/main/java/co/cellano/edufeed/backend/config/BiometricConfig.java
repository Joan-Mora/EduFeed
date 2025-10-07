package co.cellano.edufeed.backend.config;

import co.cellano.edufeed.biometric.BiometricProvider;
import co.cellano.edufeed.biometric.MockBiometricProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BiometricConfig {
    @Bean
    public BiometricProvider biometricProvider() {
        // TODO: leer variable de entorno/propiedad para seleccionar proveedor real
        return new MockBiometricProvider();
    }
}
