package com.devott.suscripciones;

import com.devott.TestcontainersConfiguration;
import com.devott.compartido.errores.DatosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SuscripcionesTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SuscripcionService suscripciones;

    @Autowired
    LimitesService limites;

    @MockitoBean
    Clock reloj;

    Clock ahora;
    UUID dueno;
    UUID vendedorId;

    @BeforeEach
    void preparar() throws Exception {
        moverA("2026-09-10");
        when(reloj.instant()).thenAnswer(i -> ahora.instant());
        when(reloj.getZone()).thenAnswer(i -> ahora.getZone());
        when(reloj.withZone(any())).thenAnswer(i -> ahora.withZone(i.getArgument(0, ZoneId.class)));
        // Las suscripciones activas de otros tests no deben vencer acá.
        jdbc.update("UPDATE suscripcion SET estado = 'CANCELADA' WHERE estado = 'ACTIVA'");

        dueno = UUID.randomUUID();
        String slug = crearVendedor(mockMvc, dueno);
        vendedorId = jdbc.queryForObject("SELECT id FROM vendedor WHERE slug = ?", UUID.class, slug);
    }

    /** Mediodía de ese día en Argentina. */
    void moverA(String fecha) {
        ahora = Clock.fixed(Instant.parse(fecha + "T15:00:00Z"), ZoneOffset.UTC);
    }

    String publicacion(String estado) {
        String slug = "test-" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO publicacion (vendedor_id, modelo_id, anio, km, condicion, precio, moneda, estado, slug)
                VALUES (?, (SELECT min(id) FROM modelo), 2021, 50000, 'USADO', 30000, 'USD', ?, ?)
                """, vendedorId, estado, slug);
        return slug;
    }

    String estado(String slug) {
        return jdbc.queryForObject("SELECT estado FROM publicacion WHERE slug = ?", String.class, slug);
    }

    @Test
    void losPlanesVanDelMasChicoAlMasGrande() {
        assertThat(suscripciones.planes()).extracting(Plan::codigo)
                .containsExactly("PARTICULAR", "CONCESIONARIA", "CONCESIONARIA_PLUS");
        assertThat(suscripciones.planes().get(1).limites()).isEqualTo(new Limites(20, 8));
    }

    @Test
    void sinPlanNoPuedePublicarPeroSiPrepararBorradores() throws Exception {
        assertThat(limites.de(vendedorId)).isEqualTo(LimitesService.SIN_PLAN);

        String id = jdbc.queryForObject("SELECT id::text FROM publicacion WHERE slug = ?", String.class,
                publicacion("BORRADOR"));
        jdbc.update("INSERT INTO foto (publicacion_id, storage_path, orden) VALUES (?::uuid, 'x.webp', 0)", id);
        mockMvc.perform(patch("/api/v1/me/publicaciones/{id}/estado", id).with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"ACTIVA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Para publicar necesitás un plan activo. Elegí uno en Planes."));
    }

    @Test
    void conPlanUsaSusLimitesTambienDuranteLaGracia() {
        suscripciones.asignar(vendedorId, "CONCESIONARIA_PLUS", LocalDate.parse("2026-10-10"), false, "MP 123");
        assertThat(limites.de(vendedorId)).isEqualTo(new Limites(100, 10));

        moverA("2026-10-17"); // último día de gracia
        assertThat(limites.de(vendedorId)).isEqualTo(new Limites(100, 10));

        moverA("2026-10-18");
        assertThat(limites.de(vendedorId)).isEqualTo(LimitesService.SIN_PLAN);
    }

    @Test
    void alPasarLaGraciaVenceYPausaLasPublicaciones() {
        suscripciones.asignar(vendedorId, "PARTICULAR", LocalDate.parse("2026-09-20"), true, null);
        String activa = publicacion("ACTIVA");
        String borrador = publicacion("BORRADOR");

        moverA("2026-09-27");
        assertThat(suscripciones.vencerAtrasadas()).isZero();
        assertThat(estado(activa)).isEqualTo("ACTIVA");

        moverA("2026-09-28");
        assertThat(suscripciones.vencerAtrasadas()).isEqualTo(1);
        assertThat(estado(activa)).isEqualTo("PAUSADA");
        assertThat(estado(borrador)).isEqualTo("BORRADOR");
        assertThat(jdbc.queryForObject("SELECT estado FROM suscripcion WHERE vendedor_id = ?", String.class, vendedorId))
                .isEqualTo("VENCIDA");
    }

    @Test
    void asignarReemplazaLaSuscripcionActiva() {
        suscripciones.asignar(vendedorId, "PARTICULAR", LocalDate.parse("2026-10-10"), true, null);
        var nueva = suscripciones.asignar(vendedorId, "CONCESIONARIA", LocalDate.parse("2026-11-10"), false, "Transf. 55");

        assertThat(suscripciones.vigente(vendedorId)).contains(nueva);
        assertThat(nueva.plan().codigo()).isEqualTo("CONCESIONARIA");
        assertThat(nueva.esPrueba()).isFalse();
        assertThat(jdbc.queryForList("SELECT estado FROM suscripcion WHERE vendedor_id = ? ORDER BY creada_en",
                String.class, vendedorId)).containsExactlyInAnyOrder("CANCELADA", "ACTIVA");
    }

    @Test
    void laBaseNoPermiteDosActivas() {
        suscripciones.asignar(vendedorId, "PARTICULAR", LocalDate.parse("2026-10-10"), false, null);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO suscripcion (vendedor_id, plan_id, estado, inicio, vence_el)
                VALUES (?, (SELECT id FROM plan WHERE codigo = 'PARTICULAR'), 'ACTIVA', current_date, current_date + 30)
                """, vendedorId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cancelarPausaEnElMomento() {
        suscripciones.asignar(vendedorId, "PARTICULAR", LocalDate.parse("2026-10-10"), false, null);
        String activa = publicacion("ACTIVA");

        suscripciones.cancelar(vendedorId);

        assertThat(suscripciones.vigente(vendedorId)).isEmpty();
        assertThat(estado(activa)).isEqualTo("PAUSADA");
    }

    @Test
    void validaPlanYFecha() {
        assertThatThrownBy(() -> suscripciones.asignar(vendedorId, "NO_EXISTE", LocalDate.parse("2026-10-10"), false, null))
                .isInstanceOf(DatosInvalidosException.class).hasMessage("No existe ese plan.");
        assertThatThrownBy(() -> suscripciones.asignar(vendedorId, "PARTICULAR", LocalDate.parse("2026-09-09"), false, null))
                .isInstanceOf(DatosInvalidosException.class);
    }
}
