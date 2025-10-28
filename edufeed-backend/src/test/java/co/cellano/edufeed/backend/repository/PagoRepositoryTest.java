package co.cellano.edufeed.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.EstadoPago;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PagoRepositoryTest {

    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    PagoRepository pagoRepository;

    @Test
    void shouldCreatePaymentForUser() {
        Usuario u = new Usuario();
        u.setDocumento("DOC-PAGO-1");
        u.setNombreCompleto("Ana Pagos");
        u.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuarioRepository.save(u);

        Pago p = new Pago();
        p.setUsuario(u);
        p.setMonto(new BigDecimal("10000"));
        p.setTipoPago(TipoPago.DIARIO);
        p.setEstadoPago(EstadoPago.APROBADO);
        Pago saved = pagoRepository.save(p);
        assertThat(saved.getId()).isNotNull();
    }
}
