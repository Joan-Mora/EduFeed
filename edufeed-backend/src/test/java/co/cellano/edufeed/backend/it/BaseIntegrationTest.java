package co.cellano.edufeed.backend.it;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Base para pruebas de integración con PostgreSQL usando Testcontainers.
 * Usa el perfil "test" y permite que Spring Boot conecte automáticamente
 * al contenedor gracias a @ServiceConnection.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainersConfig {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("edufeed")
                    .withUsername("test")
                    .withPassword("test");
        }
    }
}
