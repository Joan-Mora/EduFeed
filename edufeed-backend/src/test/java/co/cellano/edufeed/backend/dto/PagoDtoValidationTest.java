package co.cellano.edufeed.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import co.cellano.edufeed.backend.model.enums.TipoPago;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PagoDtoValidationTest {

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

    private PagoDto valido() {
        PagoDto dto = new PagoDto();
        dto.setUsuarioId("11111111-1111-1111-1111-111111111111");
        dto.setMonto(new BigDecimal("10000"));
    dto.setTipoPago(TipoPago.DIARIO);
        dto.setDiasPaquete(1); // opcional, pero válido si viene
        return dto;
    }

    @Test
    @DisplayName("DTO Pago válido no debe tener violaciones")
    void pagoValido() {
        Set<ConstraintViolation<PagoDto>> violations = validator.validate(valido());
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("usuarioId es @NotBlank y monto @Positive")
    void usuarioIdYMontoRequeridos() {
        PagoDto dto = valido();
        dto.setUsuarioId(" ");
        dto.setMonto(new BigDecimal("-5"));

        Set<ConstraintViolation<PagoDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("usuarioId"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("monto"));
    }

    @Test
    @DisplayName("tipoPago es @NotNull y diasPaquete (si viene) debe ser positivo")
    void tipoPagoObligatorioYDiasPaquetePositivo() {
        PagoDto dto = valido();
        dto.setTipoPago(null);
        dto.setDiasPaquete(0);

        Set<ConstraintViolation<PagoDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tipoPago"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("diasPaquete"));
    }
}
