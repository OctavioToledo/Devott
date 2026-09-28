package com.devott.publicaciones;

import com.devott.TestcontainersConfiguration;
import com.devott.compartido.errores.ServicioNoDisponibleException;
import com.devott.cotizaciones.Cotizacion;
import com.devott.cotizaciones.ProveedorDeCotizaciones;
import com.devott.cotizaciones.TipoCotizacion;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static com.devott.PruebasApi.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActualizacionDePreciosUsdTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ActualizacionDePreciosUsd actualizacion;

    @MockitoBean
    ProveedorDeCotizaciones proveedor;

    UUID dueno;
    int hilux;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.update("DELETE FROM cotizacion");
        dueno = UUID.randomUUID();
        crearVendedor(mockMvc, dueno);
        hilux = jdbc.queryForObject("""
                SELECT mo.id FROM modelo mo JOIN marca ma ON ma.id = mo.marca_id
                WHERE ma.slug = 'toyota' AND mo.slug = 'hilux'
                """, Integer.class);
    }

    @Test
    void alCrearEnPesosUsaLaUltimaCotizacion() throws Exception {
        guardarCotizacion(LocalDate.of(2026, 9, 1), "1400");
        guardarCotizacion(LocalDate.of(2026, 9, 2), "1500");

        String slug = crear("30000000", "ARS");

        assertThat(precioUsdRef(slug)).isEqualByComparingTo("20000");
    }

    @Test
    void alCrearEnPesosSinCotizacionQuedaSinReferencia() throws Exception {
        String slug = crear("30000000", "ARS");

        assertThat(precioUsdRef(slug)).isNull();
    }

    @Test
    void alCrearEnDolaresUsaElMismoPrecio() throws Exception {
        String slug = crear("34900", "USD");

        assertThat(precioUsdRef(slug)).isEqualByComparingTo("34900");
    }

    @Test
    void guardaLaCotizacionNuevaYRecalculaSoloLasDePesos() throws Exception {
        guardarCotizacion(LocalDate.of(2026, 9, 1), "1500");
        String enPesos = crear("30000000", "ARS");
        String enDolares = crear("34900", "USD");
        when(proveedor.actual(TipoCotizacion.BLUE))
                .thenReturn(new Cotizacion(TipoCotizacion.BLUE, LocalDate.of(2026, 9, 2), new BigDecimal("2000")));

        actualizacion.actualizar();

        assertThat(precioUsdRef(enPesos)).isEqualByComparingTo("15000");
        assertThat(precioUsdRef(enDolares)).isEqualByComparingTo("34900");
        assertThat(jdbc.queryForObject("SELECT valor FROM cotizacion WHERE fecha = '2026-09-02' AND tipo = 'BLUE'",
                BigDecimal.class)).isEqualByComparingTo("2000");
    }

    @Test
    void siElProveedorFallaRecalculaConLaUltimaGuardada() throws Exception {
        String enPesos = crear("30000000", "ARS");
        guardarCotizacion(LocalDate.of(2026, 9, 1), "1500");
        when(proveedor.actual(TipoCotizacion.BLUE))
                .thenThrow(new ServicioNoDisponibleException("caído", null));

        actualizacion.actualizar();

        assertThat(precioUsdRef(enPesos)).isEqualByComparingTo("20000");
    }

    void guardarCotizacion(LocalDate fecha, String valor) {
        jdbc.update("INSERT INTO cotizacion (fecha, tipo, valor) VALUES (?, 'BLUE', ?)", fecha, new BigDecimal(valor));
    }

    String crear(String precio, String moneda) throws Exception {
        String json = mockMvc.perform(post("/api/v1/me/publicaciones").with(usuario(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"modeloId": %d, "anio": 2021, "km": 68400, "condicion": "USADO",
                                 "precio": %s, "moneda": "%s", "carroceria": "PICKUP", "combustible": "DIESEL",
                                 "transmision": "AUTOMATICA"}
                                """.formatted(hilux, precio, moneda)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.replaceAll("(?s).*?\"slug\":\"([^\"]+)\".*", "$1");
    }

    BigDecimal precioUsdRef(String slug) {
        return jdbc.queryForObject("SELECT precio_usd_ref FROM publicacion WHERE slug = ?", BigDecimal.class, slug);
    }
}
