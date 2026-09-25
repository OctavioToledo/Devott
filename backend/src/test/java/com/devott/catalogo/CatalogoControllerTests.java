package com.devott.catalogo;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogoControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void listaLasMarcasOrdenadasSinPedirLogin() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo/marcas"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(jsonPath("$[0].nombre").value("Alfa Romeo"))
                .andExpect(jsonPath("$[*].slug", hasItem("citroen")));
    }

    @Test
    void listaLosModelosDeUnaMarca() throws Exception {
        Integer toyota = jdbc.queryForObject("SELECT id FROM marca WHERE slug = 'toyota'", Integer.class);

        mockMvc.perform(get("/api/v1/catalogo/marcas/{id}/modelos", toyota))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Hilux")))
                .andExpect(jsonPath("$[0].marcaId").value(toyota));
    }

    @Test
    void ordenaLosModelosNumericosPorSuValor() throws Exception {
        Integer peugeot = jdbc.queryForObject("SELECT id FROM marca WHERE slug = 'peugeot'", Integer.class);

        mockMvc.perform(get("/api/v1/catalogo/marcas/{id}/modelos", peugeot))
                .andExpect(jsonPath("$[0].nombre").value("206"))
                .andExpect(jsonPath("$[9].nombre").value("5008"))
                .andExpect(jsonPath("$[10].nombre").value("Boxer"));
    }

    @Test
    void marcaInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo/marcas/{id}/modelos", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la marca pedida."));
    }

    @Test
    void idNoNumericoResponde400() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo/marcas/{id}/modelos", "toyota"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parámetro 'marcaId' tiene un valor inválido."));
    }

    @Test
    void todasLasMarcasTienenModelos() {
        List<String> sinModelos = jdbc.queryForList("""
                SELECT m.slug FROM marca m
                WHERE NOT EXISTS (SELECT 1 FROM modelo mo WHERE mo.marca_id = m.id)
                """, String.class);

        assertThat(sinModelos).isEmpty();
    }

    @Test
    void losSlugsDelCatalogoSonValidos() {
        List<String> invalidos = jdbc.queryForList("""
                SELECT slug FROM marca WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
                UNION ALL
                SELECT slug FROM modelo WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
                """, String.class);

        assertThat(invalidos).isEmpty();
    }
}
