package com.devott.publicaciones;

import com.devott.TestcontainersConfiguration;
import com.devott.suscripciones.Limites;
import com.devott.suscripciones.LimitesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicacionesControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    LimitesService limites;

    UUID dueno;
    String vendedorSlug;
    int hilux;

    @BeforeEach
    void preparar() throws Exception {
        when(limites.de(any())).thenReturn(new Limites(50, 20));
        dueno = UUID.randomUUID();
        vendedorSlug = crearVendedor(mockMvc, dueno);
        hilux = jdbc.queryForObject("""
                SELECT mo.id FROM modelo mo JOIN marca ma ON ma.id = mo.marca_id
                WHERE ma.slug = 'toyota' AND mo.slug = 'hilux'
                """, Integer.class);
    }

    String cuerpo(String condicion, int km, int anio) {
        return """
                {"modeloId": %d, "version": "2.8 SRV 4x4 AT", "anio": %d, "km": %d, "condicion": "%s",
                 "precio": 34900, "moneda": "USD", "carroceria": "PICKUP", "combustible": "DIESEL",
                 "transmision": "AUTOMATICA", "traccion": "4X4", "puertas": 4, "financia": true}
                """.formatted(hilux, anio, km, condicion);
    }

    ResultActions crear(UUID usuarioId, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/me/publicaciones").with(usuario(usuarioId))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    /** Crea una publicación y devuelve {id, slug}. */
    String[] crearPublicacion(String condicion) throws Exception {
        String json = crear(dueno, cuerpo(condicion, condicion.equals("0KM") ? 0 : 68400, 2021))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new String[]{campo(json, "id"), campo(json, "slug")};
    }

    static String campo(String json, String nombre) {
        return json.replaceAll("(?s).*?\"" + nombre + "\":\"([^\"]+)\".*", "$1");
    }

    ResultActions cambiarEstado(String id, String estado) throws Exception {
        return mockMvc.perform(patch("/api/v1/me/publicaciones/{id}/estado", id).with(usuario(dueno))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"" + estado + "\"}"));
    }

    void agregarFoto(String publicacionId) {
        jdbc.update("INSERT INTO foto (publicacion_id, storage_path, orden) VALUES (?::uuid, ?, 0)",
                publicacionId, "publicaciones/" + publicacionId + "/" + UUID.randomUUID() + ".webp");
    }

    /** Crea una publicación activa y devuelve {id, slug}. */
    String[] crearActiva(String condicion) throws Exception {
        String[] publicacion = crearPublicacion(condicion);
        agregarFoto(publicacion[0]);
        cambiarEstado(publicacion[0], "ACTIVA").andExpect(status().isOk());
        return publicacion;
    }

    @Test
    void sinPerfilDeVendedorNoPuedePublicar() throws Exception {
        crear(UUID.randomUUID(), cuerpo("USADO", 1000, 2021))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Primero creá tu perfil de vendedor."));
    }

    @Test
    void creaUnBorradorConLaUbicacionDelVendedor() throws Exception {
        crear(dueno, cuerpo("USADO", 68400, 2021))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.slug", matchesPattern("^toyota-hilux-2021-[a-z0-9]{6}$")))
                .andExpect(jsonPath("$.titulo").value("Toyota Hilux"))
                .andExpect(jsonPath("$.modelo.marca").value("Toyota"))
                .andExpect(jsonPath("$.condicion").value("USADO"))
                .andExpect(jsonPath("$.traccion").value("4X4"))
                .andExpect(jsonPath("$.financia").value(true))
                .andExpect(jsonPath("$.aceptaPermuta").value(false))
                .andExpect(jsonPath("$.localidad.ciudad").value("Villa María"))
                .andExpect(jsonPath("$.localidad.lat").value(-32.41))
                .andExpect(jsonPath("$.fotos.length()").value(0))
                .andExpect(jsonPath("$.maxFotos").value(20))
                .andExpect(jsonPath("$.publicadaEn").doesNotExist());
    }

    @Test
    void validaLosDatos() throws Exception {
        crear(dueno, cuerpo("USADO", 1000, 2021).replace("\"modeloId\": " + hilux, "\"modeloId\": 999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("modeloId"));
        crear(dueno, cuerpo("0KM", 5000, 2025))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("km"));
        crear(dueno, cuerpo("USADO", 1000, 2999))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("anio"));
        crear(dueno, "{\"modeloId\": " + hilux + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'precio')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'carroceria')]").exists());
    }

    @Test
    void otroUsuarioNoPuedeVerNiTocarPublicacionesAjenas() throws Exception {
        String id = crearPublicacion("USADO")[0];
        UUID otro = UUID.randomUUID();
        crearVendedor(mockMvc, otro);

        mockMvc.perform(get("/api/v1/me/publicaciones/{id}", id).with(usuario(otro)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/me/publicaciones/{id}", id).with(usuario(otro))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo("USADO", 1, 2021)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/me/publicaciones/{id}", id).with(usuario(otro)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/me/publicaciones").with(usuario(otro)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void editarConservaElSlugYElEstado() throws Exception {
        String[] publicacion = crearPublicacion("USADO");

        mockMvc.perform(put("/api/v1/me/publicaciones/{id}", publicacion[0]).with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("USADO", 70000, 2021).replace("\"precio\": 34900", "\"precio\": 33500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(publicacion[1]))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.km").value(70000))
                .andExpect(jsonPath("$.precio").value(33500));
    }

    @Test
    void recorreLosEstadosPermitidos() throws Exception {
        String id = crearPublicacion("USADO")[0];

        cambiarEstado(id, "VENDIDA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede pasar una publicación de borrador a vendida."));
        cambiarEstado(id, "ACTIVA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Agregá al menos una foto antes de publicar."));

        agregarFoto(id);
        cambiarEstado(id, "ACTIVA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.publicadaEn").exists());
        cambiarEstado(id, "PAUSADA").andExpect(jsonPath("$.estado").value("PAUSADA"));
        cambiarEstado(id, "VENDIDA")
                .andExpect(jsonPath("$.estado").value("VENDIDA"))
                .andExpect(jsonPath("$.vendidaEn").exists());
        cambiarEstado(id, "ACTIVA")
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.vendidaEn").doesNotExist());
        cambiarEstado(id, "BORRADOR").andExpect(status().isConflict());
    }

    @Test
    void respetaElLimiteDePublicacionesActivas() throws Exception {
        when(limites.de(any())).thenReturn(new Limites(1, 20));
        crearActiva("USADO");
        String segunda = crearPublicacion("USADO")[0];
        agregarFoto(segunda);

        cambiarEstado(segunda, "ACTIVA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Llegaste al límite de 1 publicaciones activas de tu plan. Pausá o marcá como vendida alguna para publicar esta."));
    }

    @Test
    void elDetallePublicoMuestraActivasYVendidasPeroNoBorradoresNiPausadas() throws Exception {
        String[] publicacion = crearPublicacion("USADO");
        mockMvc.perform(get("/api/v1/publicaciones/{slug}", publicacion[1])).andExpect(status().isNotFound());

        agregarFoto(publicacion[0]);
        cambiarEstado(publicacion[0], "ACTIVA");
        mockMvc.perform(get("/api/v1/publicaciones/{slug}", publicacion[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Toyota Hilux"))
                .andExpect(jsonPath("$.vendedor.slug").value(vendedorSlug))
                .andExpect(jsonPath("$.vendedor.whatsapp").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        cambiarEstado(publicacion[0], "PAUSADA");
        mockMvc.perform(get("/api/v1/publicaciones/{slug}", publicacion[1])).andExpect(status().isNotFound());

        cambiarEstado(publicacion[0], "VENDIDA");
        mockMvc.perform(get("/api/v1/publicaciones/{slug}", publicacion[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VENDIDA"));
    }

    @Test
    void elStockDelVendedorMuestraSoloActivasYFiltraPorCondicion() throws Exception {
        crearActiva("USADO");
        String[] ceroKm = crearActiva("0KM");
        crearPublicacion("USADO");

        mockMvc.perform(get("/api/v1/vendedores/{slug}/publicaciones", vendedorSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].slug").value(ceroKm[1]))
                .andExpect(jsonPath("$.items[0].cantidadFotos").value(1));
        mockMvc.perform(get("/api/v1/vendedores/{slug}/publicaciones", vendedorSlug).param("condicion", "0KM"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].condicion").value("0KM"));
    }

    @Test
    void eliminaLaPublicacion() throws Exception {
        String id = crearPublicacion("USADO")[0];
        agregarFoto(id);

        mockMvc.perform(delete("/api/v1/me/publicaciones/{id}", id).with(usuario(dueno)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/me/publicaciones/{id}", id).with(usuario(dueno)))
                .andExpect(status().isNotFound());
    }
}
