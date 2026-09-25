package com.devott.interacciones;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContactoControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ContactoRepository contactos;

    UUID crearVendedor(String slug) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/me/vendedor")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "PARTICULAR", "nombrePublico": "Martín", "slug": "%s", "whatsapp": "3534123456",
                                 "localidad": {"ciudad": "Villa María", "provincia": "Córdoba", "lat": -32.41, "lng": -63.24}}
                                """.formatted(slug)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(respuesta.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));
    }

    @Test
    void registraElContactoAnonimoYRedirigeAWhatsapp() throws Exception {
        String slug = "martin-" + UUID.randomUUID().toString().substring(0, 8);
        UUID vendedorId = crearVendedor(slug);

        String destino = mockMvc.perform(get("/api/v1/contacto/vendedores/{slug}", slug))
                .andExpect(status().isFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getHeader("Location");

        var uri = UriComponentsBuilder.fromUriString(destino).build(true);
        assertThat(uri.getHost()).isEqualTo("wa.me");
        assertThat(uri.getPath()).isEqualTo("/5493534123456");
        String texto = java.net.URLDecoder.decode(uri.getQueryParams().getFirst("text"), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(texto).isEqualTo("¡Hola! Vi tu perfil en Devott (http://localhost:3000/" + slug
                + ") y quería hacerte una consulta.");

        List<Contacto> registrados = contactos.findByVendedorId(vendedorId);
        assertThat(registrados).singleElement().satisfies(c -> {
            assertThat(c.getUsuarioId()).isNull();
            assertThat(c.getPublicacionId()).isNull();
        });
    }

    @Test
    void conSesionGuardaQuienContacto() throws Exception {
        String slug = "martin-" + UUID.randomUUID().toString().substring(0, 8);
        UUID vendedorId = crearVendedor(slug);
        UUID comprador = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/contacto/vendedores/{slug}", slug)
                        .with(jwt().jwt(j -> j.subject(comprador.toString()))))
                .andExpect(status().isFound());

        assertThat(contactos.findByVendedorId(vendedorId))
                .singleElement()
                .satisfies(c -> assertThat(c.getUsuarioId()).isEqualTo(comprador));
    }

    @Test
    void vendedorInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/contacto/vendedores/{slug}", "no-existe"))
                .andExpect(status().isNotFound());
    }
}
