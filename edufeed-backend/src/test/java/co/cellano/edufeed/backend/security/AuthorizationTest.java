package co.cellano.edufeed.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.cellano.edufeed.backend.controller.AccesoController;
import co.cellano.edufeed.backend.controller.PagoController;
import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.dto.request.AccesoCheckRequest;
import co.cellano.edufeed.backend.dto.response.AccesoCheckResponse;
import co.cellano.edufeed.backend.service.AccesoService;
import co.cellano.edufeed.backend.service.PagoService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = {AccesoController.class, PagoController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtTokenProvider.class, SecurityConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthorizationTest {

    @Autowired
    MockMvc mvc;

        @TestConfiguration
        @EnableMethodSecurity
        static class MocksConfig {
                @Bean
                AccesoService accesoService() { return Mockito.mock(AccesoService.class); }
                @Bean
                PagoService pagoService() { return Mockito.mock(PagoService.class); }
        }

        @Autowired
        AccesoService accesoService;

        @Autowired
        PagoService pagoService;

        @Test
        @WithMockUser(roles = {"USER"})
        void verificarAcceso_forbidden_without_role() throws Exception {
        Mockito.when(accesoService.verificarAcceso(any(AccesoCheckRequest.class)))
                .thenReturn(AccesoCheckResponse.builder().permitido(true).motivo("OK").build());

        mvc.perform(post("/api/accesos/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"" + UUID.randomUUID() + "\",\"modalidad\":\"HUELLA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"OPERADOR_ACCESO"})
    void verificarAcceso_allowed_for_operador_acceso() throws Exception {
        Mockito.when(accesoService.verificarAcceso(any(AccesoCheckRequest.class)))
                .thenReturn(AccesoCheckResponse.builder().permitido(true).motivo("OK").build());

        mvc.perform(post("/api/accesos/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"" + UUID.randomUUID() + "\",\"modalidad\":\"HUELLA\"}"))
                .andExpect(status().isOk());
    }

        @Test
        @WithMockUser(roles = {"USER"})
        void aprobarPago_forbidden_without_role() throws Exception {
        Mockito.when(pagoService.aprobar(any(UUID.class))).thenReturn(new PagoDto());

        mvc.perform(put("/api/pagos/" + UUID.randomUUID() + "/aprobar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"OPERADOR_CAJA"})
    void aprobarPago_allowed_for_operador_caja() throws Exception {
        Mockito.when(pagoService.aprobar(any(UUID.class))).thenReturn(new PagoDto());

        mvc.perform(put("/api/pagos/" + UUID.randomUUID() + "/aprobar"))
                .andExpect(status().isOk());
    }
}
