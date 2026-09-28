package com.devott.metricas;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MetricasTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    UUID dueno;
    UUID vendedorId;
    UUID publicacionId;

    @BeforeEach
    void preparar() throws Exception {
        // Hoy es 10/9/2026 en Argentina: el período de 7 días va del 4 al 10 y el anterior del 28/8 al 3/9.
        Clock ahora = Clock.fixed(Instant.parse("2026-09-10T15:00:00Z"), ZoneOffset.UTC);
        when(reloj.instant()).thenAnswer(i -> ahora.instant());
        when(reloj.getZone()).thenAnswer(i -> ahora.getZone());
        when(reloj.withZone(any())).thenAnswer(i -> ahora.withZone(i.getArgument(0, ZoneId.class)));

        dueno = UUID.randomUUID();
        String slug = crearVendedor(mockMvc, dueno);
        vendedorId = jdbc.queryForObject("SELECT id FROM vendedor WHERE slug = ?", UUID.class, slug);
        publicacionId = jdbc.queryForObject("""
                INSERT INTO publicacion (vendedor_id, modelo_id, anio, km, condicion, precio, moneda, estado, slug)
                VALUES (?, (SELECT min(id) FROM modelo), 2021, 50000, 'USADO', 30000, 'USD', 'ACTIVA', ?)
                RETURNING id
                """, UUID.class, vendedorId, "test-" + UUID.randomUUID().toString().substring(0, 12));
    }

    void vistas(String fecha, String tipo, int cantidad) {
        jdbc.update("""
                INSERT INTO metrica_diaria (fecha, vendedor_id, publicacion_id, tipo, cantidad)
                VALUES (?::date, ?, ?, ?, ?)
                """, fecha, vendedorId, tipo.equals("VISTA_PERFIL") ? null : publicacionId, tipo, cantidad);
    }

    void contacto(String instante) {
        jdbc.update("INSERT INTO contacto (publicacion_id, vendedor_id, creado_en) VALUES (?, ?, ?::timestamptz)",
                publicacionId, vendedorId, instante);
    }

    void guardado(String instante) {
        UUID comprador = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id, email) VALUES (?, ?)", comprador, comprador + "@example.com");
        jdbc.update("INSERT INTO guardado (usuario_id, publicacion_id, creado_en) VALUES (?, ?, ?::timestamptz)",
                comprador, publicacionId, instante);
    }

    @Test
    void sumaElPeriodoYLoComparaConElAnterior() throws Exception {
        vistas("2026-09-10", "VISTA_PERFIL", 5);
        vistas("2026-09-04", "VISTA_PERFIL", 2);
        vistas("2026-09-03", "VISTA_PERFIL", 4);   // período anterior
        vistas("2026-08-27", "VISTA_PERFIL", 100); // fuera de los dos períodos
        vistas("2026-09-08", "VISTA_PUBLICACION", 7);
        contacto("2026-09-09T12:00:00-03:00");
        // 23:30 del 3/9 en Argentina ya es 4/9 en UTC: cuenta para el período anterior.
        contacto("2026-09-04T02:30:00Z");
        guardado("2026-09-10T10:00:00-03:00");

        mockMvc.perform(get("/api/v1/me/metricas").with(usuario(dueno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias").value(7))
                .andExpect(jsonPath("$.desde").value("2026-09-04"))
                .andExpect(jsonPath("$.hasta").value("2026-09-10"))
                .andExpect(jsonPath("$.actual.vistasPerfil").value(7))
                .andExpect(jsonPath("$.actual.vistasPublicaciones").value(7))
                .andExpect(jsonPath("$.actual.contactos").value(1))
                .andExpect(jsonPath("$.actual.guardados").value(1))
                .andExpect(jsonPath("$.anterior.vistasPerfil").value(4))
                .andExpect(jsonPath("$.anterior.contactos").value(1))
                .andExpect(jsonPath("$.serie", hasSize(7)))
                .andExpect(jsonPath("$.serie[0].fecha").value("2026-09-04"))
                .andExpect(jsonPath("$.serie[0].vistasPerfil").value(2))
                .andExpect(jsonPath("$.serie[1].vistasPerfil").value(0))
                .andExpect(jsonPath("$.serie[6].vistasPerfil").value(5))
                .andExpect(jsonPath("$.publicaciones[*].id", contains(publicacionId.toString())))
                .andExpect(jsonPath("$.publicaciones[0].vistas").value(7))
                .andExpect(jsonPath("$.publicaciones[0].contactos").value(1))
                .andExpect(jsonPath("$.publicaciones[0].guardados").value(1));
    }

    @Test
    void conTreintaDiasIncluyeMasHistoria() throws Exception {
        vistas("2026-08-27", "VISTA_PERFIL", 100);

        mockMvc.perform(get("/api/v1/me/metricas?dias=30").with(usuario(dueno)))
                .andExpect(jsonPath("$.desde").value("2026-08-12"))
                .andExpect(jsonPath("$.serie", hasSize(30)))
                .andExpect(jsonPath("$.actual.vistasPerfil").value(100));
    }

    @Test
    void sinActividadDevuelveCerosYNingunaPublicacion() throws Exception {
        mockMvc.perform(get("/api/v1/me/metricas").with(usuario(dueno)))
                .andExpect(jsonPath("$.actual.vistasPerfil").value(0))
                .andExpect(jsonPath("$.serie", hasSize(7)))
                .andExpect(jsonPath("$.publicaciones", hasSize(0)));
    }

    @Test
    void cadaVendedorVeSoloLoSuyo() throws Exception {
        vistas("2026-09-10", "VISTA_PERFIL", 5);
        UUID otro = UUID.randomUUID();
        crearVendedor(mockMvc, otro);

        mockMvc.perform(get("/api/v1/me/metricas").with(usuario(otro)))
                .andExpect(jsonPath("$.actual.vistasPerfil").value(0));
    }

    @Test
    void validaPeriodoPerfilYSesion() throws Exception {
        mockMvc.perform(get("/api/v1/me/metricas?dias=5").with(usuario(dueno)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("dias"));
        mockMvc.perform(get("/api/v1/me/metricas").with(usuario(UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/me/metricas")).andExpect(status().isUnauthorized());
    }
}
