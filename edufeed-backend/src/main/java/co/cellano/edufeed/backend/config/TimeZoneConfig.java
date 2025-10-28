package co.cellano.edufeed.backend.config;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

    private static final Logger log = LoggerFactory.getLogger(TimeZoneConfig.class);

    @Value("${app.timezone:America/Bogota}")
    private String appTimeZone;

    @PostConstruct
    public void init() {
        // Fija la zona horaria por defecto de la JVM (afecta LocalDateTime.now(), etc.)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(appTimeZone)));
        log.info("Zona horaria de aplicación establecida a: {}", appTimeZone);
    }
}
