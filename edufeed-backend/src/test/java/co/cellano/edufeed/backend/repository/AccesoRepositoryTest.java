package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.*;
import co.cellano.edufeed.backend.model.enums.EstadoAcceso;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.model.enums.TipoPago;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AccesoRepositoryTest {

    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    DerechoUsoRepository derechoUsoRepository;
    @Autowired
    AccesoRepository accesoRepository;

    @Test
    void shouldRegisterAccessApproved() {
        Usuario u = new Usuario();
        u.setDocumento("DOC-ACC-1");
        u.setNombreCompleto("Carlos Acceso");
        u.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuarioRepository.save(u);

        DerechoUso d = new DerechoUso();
        d.setUsuario(u);
        d.setTipoDerecho(TipoPago.MENSUAL);
        d.setVigenteDesde(OffsetDateTime.now().minusDays(1));
        d.setVigenteHasta(OffsetDateTime.now().plusDays(28));
        derechoUsoRepository.save(d);

        Acceso a = new Acceso();
        a.setUsuario(u);
        a.setDerecho(d);
        a.setEstado(EstadoAcceso.APROBADO);
        a.setModalidad(Modalidad.MANUAL);
        Acceso saved = accesoRepository.save(a);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEstado()).isEqualTo(EstadoAcceso.APROBADO);
    }
}
