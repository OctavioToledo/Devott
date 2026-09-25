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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static com.devott.publicaciones.PublicacionesControllerTests.campo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FotosControllerTests {

    static final byte[] IMAGEN = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    static final Path DIRECTORIO = Path.of("target/almacenamiento-tests");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    LimitesService limites;

    UUID dueno;
    String publicacionId;

    @BeforeEach
    void preparar() throws Exception {
        when(limites.de(any())).thenReturn(new Limites(50, 20));
        dueno = UUID.randomUUID();
        crearVendedor(mockMvc, dueno);
        Integer modelo = jdbc.queryForObject("SELECT id FROM modelo WHERE slug = 'hilux'", Integer.class);
        String json = mockMvc.perform(post("/api/v1/me/publicaciones").with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"modeloId": %d, "anio": 2021, "km": 68400, "condicion": "USADO", "precio": 34900,
                                 "moneda": "USD", "carroceria": "PICKUP", "combustible": "DIESEL", "transmision": "MANUAL"}
                                """.formatted(modelo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        publicacionId = campo(json, "id");
    }

    ResultActions pedirSubida(String contentType) throws Exception {
        return mockMvc.perform(post("/api/v1/me/publicaciones/{id}/fotos/url-subida", publicacionId).with(usuario(dueno))
                .contentType(MediaType.APPLICATION_JSON).content("{\"contentType\": \"" + contentType + "\"}"));
    }

    /** Pide la URL, sube el archivo como lo haría el navegador y devuelve la ruta. */
    String subir() throws Exception {
        String json = pedirSubida("image/webp").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        URI url = URI.create(campo(json, "url"));
        mockMvc.perform(put(url).contentType("image/webp").content(IMAGEN)).andExpect(status().isOk());
        return campo(json, "ruta");
    }

    ResultActions confirmar(String ruta) throws Exception {
        return mockMvc.perform(post("/api/v1/me/publicaciones/{id}/fotos", publicacionId).with(usuario(dueno))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ruta\": \"" + ruta + "\", \"ancho\": 1600, \"alto\": 1200}"));
    }

    String subirYConfirmar() throws Exception {
        String ruta = subir();
        confirmar(ruta).andExpect(status().isCreated());
        return ruta;
    }

    @Test
    void subeConfirmaYSirveLaFoto() throws Exception {
        pedirSubida("image/webp")
                .andExpect(jsonPath("$.metodo").value("PUT"))
                .andExpect(jsonPath("$.ruta", startsWith("publicaciones/" + publicacionId + "/")))
                .andExpect(jsonPath("$.ruta", endsWith(".webp")))
                .andExpect(jsonPath("$.headers['Content-Type']").value("image/webp"));

        String ruta = subir();
        String json = confirmar(ruta)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotos.length()").value(1))
                .andExpect(jsonPath("$.fotos[0].orden").value(0))
                .andExpect(jsonPath("$.fotos[0].ancho").value(1600))
                .andExpect(jsonPath("$.fotos[0].url").value("http://localhost:8080/almacenamiento-local/" + ruta))
                .andReturn().getResponse().getContentAsString();

        // Confirmar dos veces la misma ruta no duplica la foto.
        confirmar(ruta).andExpect(jsonPath("$.fotos.length()").value(1));

        mockMvc.perform(get("/almacenamiento-local/" + ruta))
                .andExpect(status().isOk())
                .andExpect(content().bytes(IMAGEN));
        assertThat(json).contains(ruta);
    }

    @Test
    void rechazaTiposFirmasYRutasInvalidas() throws Exception {
        pedirSubida("image/png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("contentType"));

        String json = pedirSubida("image/webp").andReturn().getResponse().getContentAsString();
        URI url = URI.create(campo(json, "url").replaceAll("firma=[^&]+", "firma=falsa"));
        mockMvc.perform(put(url).contentType("image/webp").content(IMAGEN)).andExpect(status().isForbidden());

        confirmar(campo(json, "ruta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("No encontramos la foto subida. Probá subirla de nuevo."));
        confirmar("publicaciones/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".webp")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("La foto no corresponde a esta publicación."));
    }

    @Test
    void respetaElLimiteDeFotos() throws Exception {
        when(limites.de(any())).thenReturn(new Limites(50, 1));
        subirYConfirmar();

        pedirSubida("image/webp")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Llegaste al máximo de 1 fotos por publicación de tu plan."));
    }

    @Test
    void reordenaLasFotos() throws Exception {
        String primera = subirYConfirmar();
        subirYConfirmar();
        String json = mockMvc.perform(get("/api/v1/me/publicaciones/{id}", publicacionId).with(usuario(dueno)))
                .andReturn().getResponse().getContentAsString();
        String id0 = json.replaceAll("(?s).*\"fotos\":\\[\\{\"id\":\"([^\"]+)\".*", "$1");
        String id1 = json.replaceAll("(?s).*\"fotos\":\\[\\{.*?\\},\\{\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/me/publicaciones/{id}/fotos/orden", publicacionId).with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fotoIds\": [\"" + id1 + "\", \"" + id0 + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos[0].id").value(id1))
                .andExpect(jsonPath("$.fotos[1].url", endsWith(primera)));

        mockMvc.perform(put("/api/v1/me/publicaciones/{id}/fotos/orden", publicacionId).with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fotoIds\": [\"" + id1 + "\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminaFotosPeroNoLaUnicaDeUnaActiva() throws Exception {
        String ruta = subirYConfirmar();
        mockMvc.perform(patch("/api/v1/me/publicaciones/{id}/estado", publicacionId).with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"ACTIVA\"}"))
                .andExpect(status().isOk());
        String json = mockMvc.perform(get("/api/v1/me/publicaciones/{id}", publicacionId).with(usuario(dueno)))
                .andReturn().getResponse().getContentAsString();
        String fotoId = json.replaceAll("(?s).*\"fotos\":\\[\\{\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(delete("/api/v1/me/publicaciones/{id}/fotos/{fotoId}", publicacionId, fotoId).with(usuario(dueno)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/me/publicaciones/{id}/estado", publicacionId).with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"PAUSADA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/me/publicaciones/{id}/fotos/{fotoId}", publicacionId, fotoId).with(usuario(dueno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos.length()").value(0));
        assertThat(DIRECTORIO.resolve(ruta)).doesNotExist();
    }

    @Test
    void eliminarLaPublicacionBorraSusArchivos() throws Exception {
        String ruta = subirYConfirmar();
        assertThat(Files.exists(DIRECTORIO.resolve(ruta))).isTrue();

        mockMvc.perform(delete("/api/v1/me/publicaciones/{id}", publicacionId).with(usuario(dueno)))
                .andExpect(status().isNoContent());

        assertThat(DIRECTORIO.resolve(ruta)).doesNotExist();
    }

    @Test
    void subeYReemplazaElLogoDelVendedor() throws Exception {
        String primero = subirLogo();
        mockMvc.perform(put("/api/v1/me/vendedor/logo").with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ruta\": \"" + primero + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("http://localhost:8080/almacenamiento-local/" + primero));

        String segundo = subirLogo();
        String json = mockMvc.perform(put("/api/v1/me/vendedor/logo").with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ruta\": \"" + segundo + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(DIRECTORIO.resolve(primero)).doesNotExist();

        mockMvc.perform(get("/api/v1/vendedores/{slug}", campo(json, "slug")))
                .andExpect(jsonPath("$.logoUrl", endsWith(segundo)));

        mockMvc.perform(delete("/api/v1/me/vendedor/logo").with(usuario(dueno)))
                .andExpect(jsonPath("$.logoUrl").doesNotExist());
        assertThat(DIRECTORIO.resolve(segundo)).doesNotExist();
    }

    String subirLogo() throws Exception {
        String json = mockMvc.perform(post("/api/v1/me/vendedor/logo/url-subida").with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contentType\": \"image/webp\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(put(URI.create(campo(json, "url"))).contentType("image/webp").content(IMAGEN))
                .andExpect(status().isOk());
        return campo(json, "ruta");
    }
}
