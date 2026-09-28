package com.devott.metricas;

import com.devott.TestcontainersConfiguration;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VistasTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    VistaService vistas;

    /** Reloj que se puede mover entre llamadas. */
    @MockitoBean
    Clock reloj;

    Clock ahora;
    String vendedorSlug;

    @BeforeEach
    void preparar() throws Exception {
        // 22:00 del 10/9 en Argentina (01:00 del 11/9 en UTC).
        moverA("2026-09-11T01:00:00Z");
        when(reloj.instant()).thenAnswer(i -> ahora.instant());
        when(reloj.getZone()).thenAnswer(i -> ahora.getZone());
        when(reloj.withZone(any())).thenAnswer(i -> ahora.withZone(i.getArgument(0, ZoneId.class)));
        vendedorSlug = crearVendedor(mockMvc, UUID.randomUUID());
    }

    void moverA(String instante) {
        ahora = Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
    }

    ResultActions vista(String tipo, String slug, String visitante) throws Exception {
        return mockMvc.perform(post("/api/v1/eventos/vista").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo": "%s", "slug": "%s", "visitante": "%s"}
                        """.formatted(tipo, slug, visitante)));
    }

    String publicacion(String estado) {
        String slug = "test-" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO publicacion (vendedor_id, modelo_id, anio, km, condicion, precio, moneda, estado, slug)
                VALUES ((SELECT id FROM vendedor WHERE slug = ?), (SELECT min(id) FROM modelo), 2021, 50000, 'USADO',
                        30000, 'USD', ?, ?)
                """, vendedorSlug, estado, slug);
        return slug;
    }

    Integer vistasDePerfil(String fecha) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(m.cantidad), 0)::int FROM metrica_diaria m JOIN vendedor v ON v.id = m.vendedor_id
                WHERE v.slug = ? AND m.tipo = 'VISTA_PERFIL' AND m.fecha = ?::date
                """, Integer.class, vendedorSlug, fecha);
    }

    Integer vistasDePublicacion(String slug) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(m.cantidad), 0)::int FROM metrica_diaria m JOIN publicacion p ON p.id = m.publicacion_id
                WHERE p.slug = ? AND m.tipo = 'VISTA_PUBLICACION'
                """, Integer.class, slug);
    }

    static String visitante() {
        return UUID.randomUUID().toString();
    }

    @Test
    void cuentaUnaVistaPorVisitantePorDiaConLaFechaArgentina() throws Exception {
        String juan = visitante();
        vista("VISTA_PERFIL", vendedorSlug, juan).andExpect(status().isNoContent());
        vista("VISTA_PERFIL", vendedorSlug, juan).andExpect(status().isNoContent());
        vista("VISTA_PERFIL", vendedorSlug, visitante()).andExpect(status().isNoContent());

        assertThat(vistasDePerfil("2026-09-10")).isEqualTo(2);

        // Al día siguiente el mismo visitante vuelve a contar.
        moverA("2026-09-11T15:00:00Z");
        vista("VISTA_PERFIL", vendedorSlug, juan);
        assertThat(vistasDePerfil("2026-09-11")).isEqualTo(1);
    }

    @Test
    void cuentaVistasDePublicacionesActivasSolamente() throws Exception {
        String activa = publicacion("ACTIVA");
        String vendida = publicacion("VENDIDA");
        String juan = visitante();

        vista("VISTA_PUBLICACION", activa, juan).andExpect(status().isNoContent());
        vista("VISTA_PUBLICACION", activa, juan);
        vista("VISTA_PUBLICACION", vendida, juan).andExpect(status().isNoContent());

        assertThat(vistasDePublicacion(activa)).isEqualTo(1);
        assertThat(vistasDePublicacion(vendida)).isZero();
        // Ver una publicación no suma vistas al perfil.
        assertThat(vistasDePerfil("2026-09-10")).isZero();
    }

    @Test
    void ignoraSlugsInexistentesSinDarPistas() throws Exception {
        vista("VISTA_PERFIL", "no-existe", visitante()).andExpect(status().isNoContent());
        vista("VISTA_PUBLICACION", "no-existe", visitante()).andExpect(status().isNoContent());
    }

    @Test
    void validaElPedido() throws Exception {
        vista("VISTA_PERFIL", vendedorSlug, "corto").andExpect(status().isBadRequest());
        vista("VISTA_PERFIL", vendedorSlug, "tiene espacios y cosas raras!!").andExpect(status().isBadRequest());
        vista("OTRA_COSA", vendedorSlug, visitante()).andExpect(status().isBadRequest());
    }

    @Test
    void noGuardaElIdDelVisitanteTalCual() throws Exception {
        String juan = visitante();
        vista("VISTA_PERFIL", vendedorSlug, juan);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM vista_registrada WHERE visitante = ?", Integer.class, juan))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vista_registrada WHERE visitante = ?", Integer.class,
                VistaService.hash(juan))).isEqualTo(1);
    }

    @Test
    void purgaLasVistasRegistradasDeMasDeDosDias() throws Exception {
        vista("VISTA_PERFIL", vendedorSlug, visitante());
        moverA("2026-09-14T15:00:00Z");

        vistas.purgarVistasViejas();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM vista_registrada WHERE fecha = '2026-09-10'", Integer.class))
                .isZero();
        // La métrica agregada no se toca.
        assertThat(vistasDePerfil("2026-09-10")).isEqualTo(1);
    }
}
