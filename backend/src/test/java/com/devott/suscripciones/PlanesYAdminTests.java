package com.devott.suscripciones;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlanesYAdminTests {

    /** En la configuración de tests, DEVOTT_ADMINS = admin@devott.test. */
    static final RequestPostProcessor ADMIN = jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
            .claim("email", "Admin@Devott.test"));

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    Clock reloj;

    Clock ahora;
    UUID dueno;
    String slug;

    @BeforeEach
    void preparar() throws Exception {
        moverA("2026-09-10");
        when(reloj.instant()).thenAnswer(i -> ahora.instant());
        when(reloj.getZone()).thenAnswer(i -> ahora.getZone());
        when(reloj.withZone(any())).thenAnswer(i -> ahora.withZone(i.getArgument(0, ZoneId.class)));
        dueno = UUID.randomUUID();
        slug = crearVendedor(mockMvc, dueno);
    }

    void moverA(String fecha) {
        ahora = Clock.fixed(Instant.parse(fecha + "T15:00:00Z"), ZoneOffset.UTC);
    }

    ResultActions asignar(RequestPostProcessor quien, String plan, String venceEl, boolean prueba) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/vendedores/{slug}/suscripcion", slug).with(quien)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"plan": "%s", "venceEl": "%s", "esPrueba": %s, "referencia": "Transferencia 123"}
                        """.formatted(plan, venceEl, prueba)));
    }

    ResultActions miPlan() throws Exception {
        return mockMvc.perform(get("/api/v1/me/suscripcion").with(usuario(dueno)));
    }

    @Test
    void losPlanesSonPublicos() throws Exception {
        mockMvc.perform(get("/api/v1/planes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", contains("PARTICULAR", "CONCESIONARIA", "CONCESIONARIA_PLUS")))
                .andExpect(jsonPath("$[0].maxPublicaciones").value(5))
                .andExpect(jsonPath("$[0].precioArs").value(15000));
    }

    @Test
    void sinPlanElPanelLoAvisa() throws Exception {
        miPlan().andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").doesNotExist())
                .andExpect(jsonPath("$.maxPublicaciones").value(0))
                .andExpect(jsonPath("$.avisos", contains("SIN_PLAN")));
    }

    @Test
    void elAdminAsignaUnaPruebaYElVendedorLaVe() throws Exception {
        asignar(ADMIN, "CONCESIONARIA", "2026-10-10", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.plan").value("CONCESIONARIA"))
                .andExpect(jsonPath("$.suscripcion.esPrueba").value(true))
                .andExpect(jsonPath("$.suscripcion.referencia").value("Transferencia 123"));

        miPlan().andExpect(jsonPath("$.plan.nombre").value("Concesionaria"))
                .andExpect(jsonPath("$.esPrueba").value(true))
                .andExpect(jsonPath("$.venceEl").value("2026-10-10"))
                .andExpect(jsonPath("$.pausaEl").value("2026-10-18"))
                .andExpect(jsonPath("$.diasParaVencer").value(30))
                .andExpect(jsonPath("$.maxPublicaciones").value(20))
                .andExpect(jsonPath("$.avisos").isEmpty());
    }

    @Test
    void avisaCuandoEstaPorVencerEnGraciaOEnElLimite() throws Exception {
        asignar(ADMIN, "PARTICULAR", "2026-09-15", false);
        miPlan().andExpect(jsonPath("$.avisos", contains("POR_VENCER")));

        moverA("2026-09-17");
        miPlan().andExpect(jsonPath("$.avisos", contains("EN_GRACIA")))
                .andExpect(jsonPath("$.diasParaVencer").value(-2));

        for (int i = 0; i < 5; i++) {
            jdbc.update("""
                    INSERT INTO publicacion (vendedor_id, modelo_id, anio, km, condicion, precio, moneda, estado, slug)
                    VALUES ((SELECT id FROM vendedor WHERE slug = ?), (SELECT min(id) FROM modelo), 2021, 1, 'USADO',
                            1000, 'USD', 'ACTIVA', ?)
                    """, slug, "test-" + UUID.randomUUID().toString().substring(0, 12));
        }
        miPlan().andExpect(jsonPath("$.avisos", contains("EN_GRACIA", "LIMITE_ALCANZADO")))
                .andExpect(jsonPath("$.publicacionesActivas").value(5));
    }

    @Test
    void elAdminBuscaVeVencimientosYCancela() throws Exception {
        asignar(ADMIN, "PARTICULAR", "2026-09-20", false);
        String email = dueno + "@example.com";

        mockMvc.perform(get("/api/v1/admin/vendedores").param("q", email.substring(0, 12)).with(ADMIN))
                .andExpect(jsonPath("$[*].slug", contains(slug)))
                .andExpect(jsonPath("$[0].email").value(email))
                .andExpect(jsonPath("$[0].suscripcion.nombrePlan").value("Particular"));
        mockMvc.perform(get("/api/v1/admin/suscripciones/por-vencer?dias=14").with(ADMIN))
                .andExpect(jsonPath("$[*].slug", hasItem(slug)));

        mockMvc.perform(delete("/api/v1/admin/vendedores/{slug}/suscripcion", slug).with(ADMIN))
                .andExpect(status().isNoContent());
        miPlan().andExpect(jsonPath("$.avisos", contains("SIN_PLAN")));
    }

    @Test
    void soloLosAdminsAdministran() throws Exception {
        asignar(usuario(dueno), "PLUS", "2026-10-10", false).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/vendedores").with(usuario(dueno))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/vendedores")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/me").with(ADMIN)).andExpect(jsonPath("$.admin").value(true));
        mockMvc.perform(get("/api/v1/me").with(usuario(dueno))).andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void validaElPedidoDelAdmin() throws Exception {
        asignar(ADMIN, "NO_EXISTE", "2026-10-10", false).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("plan"));
        asignar(ADMIN, "PARTICULAR", "2026-09-01", false).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("venceEl"));
    }
}
