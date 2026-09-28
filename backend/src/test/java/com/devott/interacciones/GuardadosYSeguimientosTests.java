package com.devott.interacciones;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GuardadosYSeguimientosTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    UUID dueno;
    String vendedorSlug;
    UUID comprador;
    int hilux;

    @BeforeEach
    void preparar() throws Exception {
        dueno = UUID.randomUUID();
        vendedorSlug = crearVendedor(mockMvc, dueno);
        comprador = UUID.randomUUID();
        hilux = jdbc.queryForObject("""
                SELECT mo.id FROM modelo mo JOIN marca ma ON ma.id = mo.marca_id
                WHERE ma.slug = 'toyota' AND mo.slug = 'hilux'
                """, Integer.class);
    }

    /** Publicación del vendedor del test, insertada directo en el estado pedido. Devuelve su slug. */
    String publicacion(String estado, int diasAtras) {
        String slug = "test-" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO publicacion (vendedor_id, modelo_id, anio, km, condicion, precio, moneda, estado, slug, publicada_en)
                VALUES ((SELECT id FROM vendedor WHERE slug = ?), ?, 2021, 50000, 'USADO', 30000, 'USD', ?, ?,
                        now() - make_interval(days => ?))
                """, vendedorSlug, hilux, estado, slug, diasAtras);
        return slug;
    }

    ResultActions guardar(UUID quien, String slug) throws Exception {
        return mockMvc.perform(put("/api/v1/me/guardados/{slug}", slug).with(usuario(quien)));
    }

    // Guardados --------------------------------------------------------------

    @Test
    void guardarEsIdempotenteYLaListaVaDelUltimoAlPrimero() throws Exception {
        String primera = publicacion("ACTIVA", 3);
        String segunda = publicacion("ACTIVA", 1);

        guardar(comprador, primera).andExpect(status().isNoContent());
        guardar(comprador, segunda).andExpect(status().isNoContent());
        guardar(comprador, primera).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me/guardados").with(usuario(comprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].slug", contains(segunda, primera)))
                .andExpect(jsonPath("$.items[0].titulo").value("Toyota Hilux"))
                .andExpect(jsonPath("$.total").value(2));
        mockMvc.perform(get("/api/v1/me/guardados/slugs").with(usuario(comprador)))
                .andExpect(jsonPath("$", contains(segunda, primera)));
    }

    @Test
    void quitarEsIdempotente() throws Exception {
        String slug = publicacion("ACTIVA", 1);
        guardar(comprador, slug);

        mockMvc.perform(delete("/api/v1/me/guardados/{slug}", slug).with(usuario(comprador)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/me/guardados/{slug}", slug).with(usuario(comprador)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/me/guardados/{slug}", "no-existe").with(usuario(comprador)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me/guardados/slugs").with(usuario(comprador)))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void soloSePuedenGuardarLasVisibles() throws Exception {
        guardar(comprador, publicacion("BORRADOR", 1)).andExpect(status().isNotFound());
        guardar(comprador, publicacion("PAUSADA", 1)).andExpect(status().isNotFound());
        guardar(comprador, "no-existe").andExpect(status().isNotFound());
        guardar(comprador, publicacion("VENDIDA", 1)).andExpect(status().isNoContent());
    }

    @Test
    void unaPausadaDesapareceYVuelveAlReactivarse() throws Exception {
        String slug = publicacion("ACTIVA", 1);
        guardar(comprador, slug);

        jdbc.update("UPDATE publicacion SET estado = 'PAUSADA' WHERE slug = ?", slug);
        mockMvc.perform(get("/api/v1/me/guardados").with(usuario(comprador)))
                .andExpect(jsonPath("$.items", empty())).andExpect(jsonPath("$.total").value(0));

        jdbc.update("UPDATE publicacion SET estado = 'VENDIDA' WHERE slug = ?", slug);
        mockMvc.perform(get("/api/v1/me/guardados").with(usuario(comprador)))
                .andExpect(jsonPath("$.items[0].estado").value("VENDIDA"));
    }

    @Test
    void cadaUnoVeSoloLoSuyo() throws Exception {
        String slug = publicacion("ACTIVA", 1);
        guardar(comprador, slug);

        mockMvc.perform(get("/api/v1/me/guardados/slugs").with(usuario(UUID.randomUUID())))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void paginaLosGuardados() throws Exception {
        String a = publicacion("ACTIVA", 1);
        String b = publicacion("ACTIVA", 1);
        String c = publicacion("ACTIVA", 1);
        for (String slug : new String[]{a, b, c}) {
            guardar(comprador, slug);
        }

        mockMvc.perform(get("/api/v1/me/guardados?tamano=2").with(usuario(comprador)))
                .andExpect(jsonPath("$.items[*].slug", contains(c, b)))
                .andExpect(jsonPath("$.hayMas").value(true));
        mockMvc.perform(get("/api/v1/me/guardados?tamano=2&pagina=1").with(usuario(comprador)))
                .andExpect(jsonPath("$.items[*].slug", contains(a)))
                .andExpect(jsonPath("$.hayMas").value(false));
    }

    // Seguimientos -----------------------------------------------------------

    @Test
    void seguirEsIdempotenteYMuestraLosAutosActivos() throws Exception {
        publicacion("ACTIVA", 1);
        publicacion("ACTIVA", 2);
        publicacion("PAUSADA", 1);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(put("/api/v1/me/seguimientos/{slug}", vendedorSlug).with(usuario(comprador)))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get("/api/v1/me/seguimientos").with(usuario(comprador)))
                .andExpect(jsonPath("$[*].slug", contains(vendedorSlug)))
                .andExpect(jsonPath("$[0].nombrePublico").value("Automotores del Sur"))
                .andExpect(jsonPath("$[0].ciudad").value("Villa María"))
                .andExpect(jsonPath("$[0].autosActivos").value(2));
        mockMvc.perform(get("/api/v1/me/seguimientos/slugs").with(usuario(comprador)))
                .andExpect(jsonPath("$", contains(vendedorSlug)));

        mockMvc.perform(delete("/api/v1/me/seguimientos/{slug}", vendedorSlug).with(usuario(comprador)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/me/seguimientos/{slug}", vendedorSlug).with(usuario(comprador)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/me/seguimientos/slugs").with(usuario(comprador)))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void noSePuedeSeguirElPerfilPropioNiUnoInexistente() throws Exception {
        mockMvc.perform(put("/api/v1/me/seguimientos/{slug}", vendedorSlug).with(usuario(dueno)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No podés seguir tu propio perfil."));
        mockMvc.perform(put("/api/v1/me/seguimientos/{slug}", "no-existe").with(usuario(comprador)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinSesionResponde401() throws Exception {
        String slug = publicacion("ACTIVA", 1);
        mockMvc.perform(get("/api/v1/me/guardados")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/me/guardados/{slug}", slug)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/me/seguimientos/{slug}", vendedorSlug)).andExpect(status().isUnauthorized());
    }
}
