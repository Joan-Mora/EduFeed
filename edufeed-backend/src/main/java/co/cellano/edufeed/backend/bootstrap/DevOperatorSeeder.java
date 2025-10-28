package co.cellano.edufeed.backend.bootstrap;

import co.cellano.edufeed.backend.domain.Operador;
import co.cellano.edufeed.backend.repository.OperadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea un operador por defecto en desarrollo si no existe.
 * Controlado por propiedades app.seed.operadores.* en application.yml
 */
@Component
public class DevOperatorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevOperatorSeeder.class);

    private final OperadorRepository operadorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.operadores.enabled:true}")
    private boolean enabled;

    @Value("${app.seed.operadores.username:admin}")
    private String username;

    @Value("${app.seed.operadores.password:Admin123$}")
    private String password;

    @Value("${app.seed.operadores.roles:ROLE_ADMIN}")
    private String roles;

    public DevOperatorSeeder(OperadorRepository operadorRepository, PasswordEncoder passwordEncoder) {
        this.operadorRepository = operadorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.debug("Seeder de operadores deshabilitado (app.seed.operadores.enabled=false)");
            return;
        }
        operadorRepository.findByUsername(username).ifPresentOrElse(op -> {
            log.info("Operador por defecto ya existe: {} (activo={})", op.getUsername(), op.isActivo());
        }, () -> {
            Operador op = new Operador();
            op.setUsername(username);
            op.setPasswordHash(passwordEncoder.encode(password));
            op.setRoles(roles);
            op.setActivo(true);
            operadorRepository.save(op);
            log.info("Operador por defecto creado: {} con roles {}", username, roles);
        });
    }
}
