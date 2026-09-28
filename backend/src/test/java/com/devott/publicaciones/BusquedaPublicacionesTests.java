package com.devott.publicaciones;

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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.devott.PruebasApi.crearVendedor;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BusquedaPublicacionesTests {

    // Villa María es donde está el vendedor de PruebasApi; Córdoba queda a ~140 km y Rosario a ~250 km.
    static final double[] VILLA_MARIA = {-32.41, -63.24};
    static final double[] CORDOBA = {-31.42, -64.18};
    static final double[] ROSARIO = {-32.95, -60.65};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    UUID concesionaria;
    UUID particular;
    Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void preparar() throws Exception {
        // Las publicaciones activas de otros tests no deben aparecer en estos resultados.
        jdbc.update("UPDATE publicacion SET estado = 'PAUSADA' WHERE estado = 'ACTIVA'");
        UUID usuario = UUID.randomUUID();
        concesionaria = vendedorId(crearVendedor(mockMvc, usuario));
        UUID otroUsuario = UUID.randomUUID();
        particular = vendedorId(crearVendedor(mockMvc, otroUsuario));
        jdbc.update("UPDATE vendedor SET tipo = 'PARTICULAR' WHERE id = ?", particular);
    }

    @Test
    void muestraSoloActivasLasMasRecientesPrimero() throws Exception {
        String vieja = publicar(builder().publicadaHaceDias(5));
        String nueva = publicar(builder().publicadaHaceDias(1));
        publicar(builder().estado("BORRADOR"));
        publicar(builder().estado("VENDIDA"));

        buscar("").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].slug", contains(nueva, vieja)))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.hayMas").value(false))
                .andExpect(jsonPath("$.items[0].distanciaKm").value(nullValue()));
    }

    @Test
    void filtraPorRadioEInformaLaDistancia() throws Exception {
        String cerca = publicar(builder().en(VILLA_MARIA));
        String cordoba = publicar(builder().en(CORDOBA));
        publicar(builder().en(ROSARIO));
        publicar(builder().en(null));

        buscar("lat=-32.41&lng=-63.24&radioKm=200&orden=CERCANIA")
                .andExpect(jsonPath("$.items[*].slug", contains(cerca, cordoba)))
                .andExpect(jsonPath("$.items[0].distanciaKm").value(closeTo(0.0, 0.1)))
                .andExpect(jsonPath("$.items[1].distanciaKm").value(closeTo(140.0, 10.0)));
    }

    @Test
    void filtraPorCaracteristicas() throws Exception {
        String buscada = publicar(builder().modelo("toyota", "hilux").condicion("USADO").anio(2021).km(60000)
                .carroceria("PICKUP").combustible("DIESEL").transmision("AUTOMATICA")
                .financia().permuta().unicoDueno());
        publicar(builder().modelo("toyota", "corolla"));
        publicar(builder().modelo("toyota", "hilux").condicion("0KM").km(0).carroceria("PICKUP"));
        publicar(builder().modelo("toyota", "hilux").anio(2015).carroceria("PICKUP"));
        publicar(builder().modelo("toyota", "hilux").km(150000).carroceria("PICKUP"));

        buscar("marca=toyota&modelo=hilux&condicion=USADO&anioMin=2020&kmMax=100000"
                + "&carroceria=PICKUP,SUV&combustible=DIESEL&transmision=AUTOMATICA"
                + "&financia=true&permuta=true&unicoDueno=true")
                .andExpect(jsonPath("$.items[*].slug", contains(buscada)));
        buscar("marca=toyota&carroceria=SEDAN").andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void filtraPorTipoDeVendedor() throws Exception {
        String deParticular = publicar(builder().de(particular));
        publicar(builder());

        buscar("tipoVendedor=PARTICULAR").andExpect(jsonPath("$.items[*].slug", contains(deParticular)));
    }

    @Test
    void filtraYOrdenaPorPrecioEnDolaresSinImportarLaMoneda() throws Exception {
        jdbc.update("DELETE FROM cotizacion");
        jdbc.update("INSERT INTO cotizacion (fecha, tipo, valor) VALUES ('2026-01-01', 'BLUE', 1000)");
        String barata = publicar(builder().precio("10000000", "ARS", "10000"));
        String media = publicar(builder().precio("20000", "USD", "20000"));
        String cara = publicar(builder().precio("40000", "USD", "40000"));

        buscar("orden=PRECIO_ASC").andExpect(jsonPath("$.items[*].slug", contains(barata, media, cara)));
        buscar("orden=PRECIO_DESC").andExpect(jsonPath("$.items[*].slug", contains(cara, media, barata)));
        buscar("precioMin=15000&precioMax=30000").andExpect(jsonPath("$.items[*].slug", contains(media)));
        // En pesos: 15 a 45 millones = 15.000 a 45.000 dólares.
        buscar("precioMin=15000000&precioMax=45000000&moneda=ARS")
                .andExpect(jsonPath("$.items[*].slug", containsInAnyOrder(media, cara)))
                .andExpect(jsonPath("$.items[?(@.slug == '" + barata + "')]").value(empty()));
        // La respuesta mantiene el precio original.
        buscar("orden=PRECIO_ASC").andExpect(jsonPath("$.items[0].precio").value(10000000))
                .andExpect(jsonPath("$.items[0].moneda").value("ARS"));
    }

    @Test
    void ordenaPorKmYPorAnio() throws Exception {
        String muchos = publicar(builder().km(90000).anio(2022));
        String pocos = publicar(builder().km(10000).anio(2018));

        buscar("orden=KM_ASC").andExpect(jsonPath("$.items[*].slug", contains(pocos, muchos)));
        buscar("orden=ANIO_DESC").andExpect(jsonPath("$.items[*].slug", contains(muchos, pocos)));
    }

    @Test
    void pagina() throws Exception {
        String a = publicar(builder().publicadaHaceDias(1));
        String b = publicar(builder().publicadaHaceDias(2));
        String c = publicar(builder().publicadaHaceDias(3));

        buscar("tamano=2").andExpect(jsonPath("$.items[*].slug", contains(a, b)))
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.hayMas").value(true));
        buscar("tamano=2&pagina=1").andExpect(jsonPath("$.items[*].slug", contains(c)))
                .andExpect(jsonPath("$.hayMas").value(false));
        buscar("tamano=2&pagina=5").andExpect(jsonPath("$.items").value(empty()))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void validaLosFiltros() throws Exception {
        buscar("lat=-32.41&lng=-63.24").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("radioKm"));
        buscar("orden=CERCANIA").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("orden"));
        buscar("modelo=hilux").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("modelo"));
        buscar("precioMin=100&precioMax=50").andExpect(status().isBadRequest());
        buscar("tamano=500").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("tamano"));
        buscar("carroceria=TANQUE").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("carroceria"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Valor inválido."));
    }

    // Auxiliares ------------------------------------------------------------

    ResultActions buscar(String query) throws Exception {
        return mockMvc.perform(get("/api/v1/publicaciones?" + query));
    }

    UUID vendedorId(String slug) {
        return jdbc.queryForObject("SELECT id FROM vendedor WHERE slug = ?", UUID.class, slug);
    }

    Publicar builder() {
        return new Publicar();
    }

    String publicar(Publicar p) {
        String slug = "test-" + UUID.randomUUID().toString().substring(0, 12);
        Integer modeloId = jdbc.queryForObject("""
                SELECT mo.id FROM modelo mo JOIN marca ma ON ma.id = mo.marca_id WHERE ma.slug = ? AND mo.slug = ?
                """, Integer.class, p.marca, p.modelo);
        jdbc.update("""
                        INSERT INTO publicacion (vendedor_id, modelo_id, anio, km, condicion, precio, moneda, precio_usd_ref,
                            carroceria, combustible, transmision, financia, acepta_permuta, unico_dueno, ubicacion,
                            estado, slug, publicada_en)
                        VALUES (?, ?, ?, ?, ?, ?::numeric, ?, ?::numeric, ?, ?, ?, ?, ?, ?,
                            CASE WHEN ?::float8 IS NULL THEN NULL
                                 ELSE ST_SetSRID(ST_MakePoint(?::float8, ?::float8), 4326)::geography END,
                            ?, ?, ?)
                        """,
                p.vendedor == null ? concesionaria : p.vendedor, modeloId, p.anio, p.km, p.condicion, p.precio,
                p.moneda, p.precioUsd, p.carroceria, p.combustible, p.transmision, p.financia, p.permuta,
                p.unicoDueno, p.lugar == null ? null : p.lugar[1], p.lugar == null ? null : p.lugar[1],
                p.lugar == null ? null : p.lugar[0], p.estado, slug,
                java.sql.Timestamp.from(ahora.minus(p.diasPublicada, ChronoUnit.DAYS)));
        return slug;
    }

    /** Publicación de prueba con valores por defecto razonables. */
    class Publicar {
        UUID vendedor;
        String marca = "toyota";
        String modelo = "corolla";
        int anio = 2020;
        int km = 50000;
        String condicion = "USADO";
        String precio = "20000";
        String moneda = "USD";
        String precioUsd = "20000";
        String carroceria = "SEDAN";
        String combustible = "NAFTA";
        String transmision = "MANUAL";
        boolean financia;
        boolean permuta;
        boolean unicoDueno;
        double[] lugar = VILLA_MARIA;
        String estado = "ACTIVA";
        int diasPublicada = 1;

        Publicar de(UUID v) { vendedor = v; return this; }
        Publicar modelo(String ma, String mo) { marca = ma; modelo = mo; return this; }
        Publicar anio(int a) { anio = a; return this; }
        Publicar km(int k) { km = k; return this; }
        Publicar condicion(String c) { condicion = c; return this; }
        Publicar precio(String p, String m, String usd) { precio = p; moneda = m; precioUsd = usd; return this; }
        Publicar carroceria(String c) { carroceria = c; return this; }
        Publicar combustible(String c) { combustible = c; return this; }
        Publicar transmision(String t) { transmision = t; return this; }
        Publicar financia() { financia = true; return this; }
        Publicar permuta() { permuta = true; return this; }
        Publicar unicoDueno() { unicoDueno = true; return this; }
        Publicar en(double[] l) { lugar = l; return this; }
        Publicar estado(String e) { estado = e; return this; }
        Publicar publicadaHaceDias(int d) { diasPublicada = d; return this; }
    }
}
