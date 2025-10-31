package co.cellano.edufeed.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.model.enums.TipoUsuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsuarioDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) factory.close();
    }

    private UsuarioDto valido() {
        UsuarioDto dto = new UsuarioDto();
        dto.setDocumento("1234567890");
        dto.setNombreCompleto("Juan Pérez");
        dto.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        dto.setEmail("juan@example.com");
        dto.setTelefono("3001234567");
        dto.setActivo(true);
        return dto;
    }

    @Test
    @DisplayName("DTO Usuario válido no debe tener violaciones")
    void usuarioValido() {
        Set<ConstraintViolation<UsuarioDto>> violations = validator.validate(valido());
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Documento y nombre son @NotBlank")
    void notBlankDocumentoNombre() {
        UsuarioDto dto = valido();
        dto.setDocumento(" ");
        dto.setNombreCompleto(null);

        Set<ConstraintViolation<UsuarioDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("documento"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nombreCompleto"));
    }

    @Test
    @DisplayName("tipoUsuario es @NotNull y email debe ser @Email si no es nulo")
    void tipoUsuarioNotNullYEmailValido() {
        UsuarioDto dto = valido();
        dto.setTipoUsuario(null);
        dto.setEmail("correo-invalido");

        Set<ConstraintViolation<UsuarioDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tipoUsuario"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
