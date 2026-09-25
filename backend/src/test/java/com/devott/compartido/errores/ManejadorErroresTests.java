package com.devott.compartido.errores;

import com.devott.TestcontainersConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ManejadorErroresTests.ControladorDePrueba.class})
class ManejadorErroresTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void sinTokenResponde401ConProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/no-encontrado"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.detail").value("Necesitás iniciar sesión."));
    }

    @Test
    void rutaPublicaInexistenteResponde404SinPedirLogin() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("No encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el recurso pedido."));
    }

    @Test
    void recursoNoEncontradoResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/no-encontrado").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la publicación."));
    }

    @Test
    void conflictoResponde409() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/conflicto").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto"))
                .andExpect(jsonPath("$.detail").value("El slug ya está en uso."));
    }

    @Test
    void errorInesperadoResponde500SinExponerDetalles() throws Exception {
        mockMvc.perform(get("/api/v1/prueba/explota").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado. Probá de nuevo más tarde."));
    }

    @Test
    void datosInvalidosResponden400ConErroresPorCampo() throws Exception {
        mockMvc.perform(post("/api/v1/prueba/validar").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("no debe estar vacío"));
    }

    @Test
    void jsonMalformadoResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/prueba/validar").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nombre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud no es un JSON válido."));
    }

    @TestConfiguration
    @RestController
    static class ControladorDePrueba {

        record Datos(@NotBlank String nombre) {
        }

        @GetMapping("/api/v1/prueba/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("No existe la publicación.");
        }

        @GetMapping("/api/v1/prueba/conflicto")
        void conflicto() {
            throw new ConflictoException("El slug ya está en uso.");
        }

        @GetMapping("/api/v1/prueba/explota")
        void explota() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }

        @PostMapping("/api/v1/prueba/validar")
        void validar(@Valid @RequestBody Datos datos) {
        }
    }
}
