package co.cellano.edufeed.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.model.Usuario;
import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Test
    void shouldCreateAndFindByDocumento() {
        Usuario u = new Usuario();
        u.setDocumento("DOC-123");
        u.setNombreCompleto("Usuario Prueba");
        u.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        Usuario saved = usuarioRepository.save(u);
        assertThat(saved.getId()).isNotNull();

        assertThat(usuarioRepository.findByDocumento("DOC-123")).isPresent();
    }
}
