package com.devott.vendedores;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.devott.vendedores.MiVendedorControllerTests.cuerpo;
import static com.devott.vendedores.MiVendedorControllerTests.unicoSlug;
import static com.devott.vendedores.MiVendedorControllerTests.usuario;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VendedorPublicoControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void muestraElPerfilSinLoginYSinElNumero() throws Exception {
        String slug = unicoSlug();
        mockMvc.perform(post("/api/v1/me/vendedor").with(usuario(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(slug, "3534123456")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/vendedores/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombrePublico").value("Automotores del Sur"))
                .andExpect(jsonPath("$.ciudad").value("Villa María"))
                .andExpect(jsonPath("$.tipo").value("CONCESIONARIA"))
                .andExpect(jsonPath("$.whatsapp").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void vendedorInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/vendedores/{slug}", "no-existe-" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe ese vendedor."));
    }
}
