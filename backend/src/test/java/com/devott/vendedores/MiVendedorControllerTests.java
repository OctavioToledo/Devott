package com.devott.vendedores;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiVendedorControllerTests {

    @Autowired
    MockMvc mockMvc;

    static RequestPostProcessor usuario(UUID id) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("email", id + "@example.com"));
    }

    static String unicoSlug() {
        return "autos-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static String cuerpo(String slug, String whatsapp) {
        return """
                {
                  "tipo": "CONCESIONARIA",
                  "nombrePublico": "  Automotores del Sur ",
                  "slug": "%s",
                  "whatsapp": "%s",
                  "descripcion": "Usados seleccionados.",
                  "direccion": "Bv. España 123",
                  "localidad": {"ciudad": "Villa María", "provincia": "Córdoba", "lat": -32.41, "lng": -63.24},
                  "horarios": "Lun a vie de 9 a 18",
                  "instagram": "https://instagram.com/autos.sur",
                  "facebook": ""
                }
                """.formatted(slug, whatsapp);
    }

    ResultActions crear(UUID usuarioId, String slug) throws Exception {
        return mockMvc.perform(post("/api/v1/me/vendedor").with(usuario(usuarioId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(slug, "0353 4123456")));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/me/vendedor")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinPerfilResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/me/vendedor").with(usuario(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Todavía no creaste tu perfil de vendedor."));
    }

    @Test
    void creaElPerfilNormalizandoLosDatos() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        String slug = unicoSlug();

        crear(usuarioId, slug)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.nombrePublico").value("Automotores del Sur"))
                .andExpect(jsonPath("$.whatsapp").value("3534123456"))
                .andExpect(jsonPath("$.instagram").value("autos.sur"))
                .andExpect(jsonPath("$.facebook").doesNotExist())
                .andExpect(jsonPath("$.localidad.lat").value(-32.41))
                .andExpect(jsonPath("$.localidad.lng").value(-63.24))
                .andExpect(jsonPath("$.verificado").value(false));

        mockMvc.perform(get("/api/v1/me/vendedor").with(usuario(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    void noPermiteDosPerfilesPorUsuario() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        crear(usuarioId, unicoSlug()).andExpect(status().isCreated());

        crear(usuarioId, unicoSlug())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya tenés un perfil de vendedor."));
    }

    @Test
    void rechazaSlugsReservadosOUsados() throws Exception {
        crear(UUID.randomUUID(), "panel")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("slug"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Ese link está reservado. Elegí otro."));

        String slug = unicoSlug();
        crear(UUID.randomUUID(), slug).andExpect(status().isCreated());
        crear(UUID.randomUUID(), slug)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores[0].campo").value("slug"));
    }

    @Test
    void rechazaWhatsappInvalidoYCamposFaltantes() throws Exception {
        mockMvc.perform(post("/api/v1/me/vendedor").with(usuario(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(unicoSlug(), "15 4123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("whatsapp"));

        mockMvc.perform(post("/api/v1/me/vendedor").with(usuario(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"PARTICULAR\", \"slug\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombrePublico')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'localidad')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'slug')]").exists());
    }

    @Test
    void editaElPerfilYPuedeConservarSuSlug() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        String slug = unicoSlug();
        crear(usuarioId, slug).andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/me/vendedor").with(usuario(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(slug, "11 4567 8901").replace("Automotores del Sur", "Autos Sur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombrePublico").value("Autos Sur"))
                .andExpect(jsonPath("$.whatsapp").value("1145678901"));
    }

    @Test
    void informaSiUnSlugEstaDisponible() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        String propio = unicoSlug();
        crear(usuarioId, propio).andExpect(status().isCreated());
        String ajeno = unicoSlug();
        crear(UUID.randomUUID(), ajeno).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/vendedor/slug-disponible").param("slug", propio).with(usuario(usuarioId)))
                .andExpect(jsonPath("$.disponible").value(true));
        mockMvc.perform(get("/api/v1/me/vendedor/slug-disponible").param("slug", ajeno).with(usuario(usuarioId)))
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.motivo").value("Ese link ya está en uso. Elegí otro."));
        mockMvc.perform(get("/api/v1/me/vendedor/slug-disponible").param("slug", "cuenta").with(usuario(usuarioId)))
                .andExpect(jsonPath("$.disponible").value(false));
    }
}
