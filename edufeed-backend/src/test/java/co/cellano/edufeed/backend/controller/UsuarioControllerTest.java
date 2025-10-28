package co.cellano.edufeed.backend.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.dto.request.BiometricEnrollRequest;
import co.cellano.edufeed.backend.model.enums.Modalidad;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import co.cellano.edufeed.backend.service.BiometricService;
import co.cellano.edufeed.backend.service.PlantillaBiometricaService;
import co.cellano.edufeed.backend.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private BiometricService biometricService;

    @MockBean
    private PlantillaBiometricaService plantillaBiometricaService;

    @MockBean
    private PlantillaBiometricaRepository plantillaBiometricaRepository;

    @Test
    @DisplayName("POST /api/usuarios crea usuario y retorna 201")
    void crearUsuario() throws Exception {
        UsuarioDto req = new UsuarioDto();
        req.setDocumento("123");
        req.setNombreCompleto("Juan Perez");
        req.setEmail("juan@example.com");
        req.setTelefono("3001234567");
        req.setActivo(true);

        UsuarioDto resp = new UsuarioDto();
        resp.setId(UUID.randomUUID().toString());
        resp.setDocumento(req.getDocumento());
        resp.setNombreCompleto(req.getNombreCompleto());
        resp.setEmail(req.getEmail());
        resp.setTelefono(req.getTelefono());
        resp.setActivo(true);

    Mockito.when(usuarioService.create(org.mockito.ArgumentMatchers.any(UsuarioDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.documento", is("123")))
                .andExpect(jsonPath("$.nombreCompleto", is("Juan Perez")));
    }

    @Test
    @DisplayName("GET /api/usuarios?page=&size= retorna paginado")
    void listarUsuariosPaginado() throws Exception {
        UsuarioDto u1 = new UsuarioDto();
        u1.setId(UUID.randomUUID().toString());
        u1.setDocumento("111");
        u1.setNombreCompleto("A");

        UsuarioDto u2 = new UsuarioDto();
        u2.setId(UUID.randomUUID().toString());
        u2.setDocumento("222");
        u2.setNombreCompleto("B");

        Page<UsuarioDto> page = new PageImpl<>(List.of(u1, u2), PageRequest.of(0, 2), 2);
    Mockito.when(usuarioService.list(org.mockito.ArgumentMatchers.any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/usuarios")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("POST /api/usuarios/{id}/biometria/enrolar retorna 201")
    void enrolarBiometria() throws Exception {
        BiometricEnrollRequest req = new BiometricEnrollRequest();
        req.setModalidad(Modalidad.ROSTRO);

    // Simular que el servicio devuelve una entidad enrolada; el controller mapea a DTO
    co.cellano.edufeed.backend.model.PlantillaBiometrica entidad = new co.cellano.edufeed.backend.model.PlantillaBiometrica();
    java.lang.reflect.Field f = co.cellano.edufeed.backend.model.PlantillaBiometrica.class.getDeclaredField("id");
    f.setAccessible(true);
    f.set(entidad, java.util.UUID.randomUUID());
    entidad.setModalidad(Modalidad.ROSTRO);
    entidad.setProveedor("mock");

    Mockito.when(biometricService.enrolar(org.mockito.ArgumentMatchers.<UUID>any(), eq(Modalidad.ROSTRO)))
        .thenReturn(entidad);

        mockMvc.perform(post("/api/usuarios/{id}/biometria/enrolar", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidad", is("ROSTRO")))
                .andExpect(jsonPath("$.proveedor", is("mock")));
    }
}
