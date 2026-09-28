package com.devott.interacciones;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    JdbcTemplate jdbc;

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

    // Por publicación -------------------------------------------------------

    /** Crea un vendedor con una publicación en el estado pedido y devuelve {publicacionId, slug}. */
    String[] crearPublicacion(String estado, String version) throws Exception {
        UUID dueno = UUID.randomUUID();
        String slug = com.devott.PruebasApi.crearVendedor(mockMvc, dueno);
        int hilux = jdbc.queryForObject("""
                SELECT mo.id FROM modelo mo JOIN marca ma ON ma.id = mo.marca_id
                WHERE ma.slug = 'toyota' AND mo.slug = 'hilux'
                """, Integer.class);
        String json = mockMvc.perform(post("/api/v1/me/publicaciones").with(com.devott.PruebasApi.usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"modeloId": %d, %s "anio": 2021, "km": 68400, "condicion": "USADO",
                                 "precio": 34900, "moneda": "USD", "carroceria": "PICKUP", "combustible": "DIESEL",
                                 "transmision": "AUTOMATICA"}
                                """.formatted(hilux, version == null ? "" : "\"version\": \"" + version + "\",")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll("(?s).*?\"id\":\"([^\"]+)\".*", "$1");
        String publicacionSlug = json.replaceAll("(?s).*?\"slug\":\"([^\"]+)\".*", "$1");
        jdbc.update("UPDATE publicacion SET estado = ? WHERE id = ?::uuid", estado, id);
        assertThat(slug).isNotBlank();
        return new String[]{id, publicacionSlug};
    }

    @Test
    void porPublicacionRegistraElContactoYMandaElMensajeDelAuto() throws Exception {
        String[] publicacion = crearPublicacion("ACTIVA", "2.8 SRV 4x4 AT");

        String destino = mockMvc.perform(get("/api/v1/contacto/publicaciones/{slug}", publicacion[1]))
                .andExpect(status().isFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getHeader("Location");

        var uri = UriComponentsBuilder.fromUriString(destino).build(true);
        assertThat(uri.getHost()).isEqualTo("wa.me");
        assertThat(uri.getPath()).isEqualTo("/5493534123456");
        String texto = java.net.URLDecoder.decode(uri.getQueryParams().getFirst("text"), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(texto).isEqualTo("¡Hola! Vi tu Toyota Hilux 2.8 SRV 4x4 AT 2021 publicado en Devott a US$ 34.900 "
                + "y quería consultarte si sigue disponible.\nhttp://localhost:3000/publicaciones/" + publicacion[1]);

        UUID publicacionId = UUID.fromString(publicacion[0]);
        assertThat(contactos.findAll()).filteredOn(c -> publicacionId.equals(c.getPublicacionId()))
                .singleElement()
                .satisfies(c -> assertThat(c.getUsuarioId()).isNull());
    }

    @Test
    void porPublicacionConSesionGuardaQuienContacto() throws Exception {
        String[] publicacion = crearPublicacion("ACTIVA", null);
        UUID comprador = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/contacto/publicaciones/{slug}", publicacion[1])
                        .with(jwt().jwt(j -> j.subject(comprador.toString()))))
                .andExpect(status().isFound());

        UUID publicacionId = UUID.fromString(publicacion[0]);
        assertThat(contactos.findAll()).filteredOn(c -> publicacionId.equals(c.getPublicacionId()))
                .singleElement()
                .satisfies(c -> assertThat(c.getUsuarioId()).isEqualTo(comprador));
    }

    @Test
    void porPublicacionNoActivaOInexistenteResponde404SinRegistrar() throws Exception {
        for (String estado : List.of("BORRADOR", "PAUSADA", "VENDIDA")) {
            String[] publicacion = crearPublicacion(estado, null);
            mockMvc.perform(get("/api/v1/contacto/publicaciones/{slug}", publicacion[1]))
                    .andExpect(status().isNotFound());
            UUID publicacionId = UUID.fromString(publicacion[0]);
            assertThat(contactos.findAll()).noneMatch(c -> publicacionId.equals(c.getPublicacionId()));
        }
        mockMvc.perform(get("/api/v1/contacto/publicaciones/{slug}", "no-existe"))
                .andExpect(status().isNotFound());
    }
}
