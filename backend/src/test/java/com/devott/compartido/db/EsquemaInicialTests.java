package com.devott.compartido.db;

import com.devott.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EsquemaInicialTests {

    static final List<String> TABLAS = List.of(
            "usuario", "vendedor", "plan", "suscripcion", "marca", "modelo", "publicacion", "foto",
            "guardado", "seguimiento", "contacto", "resena", "metrica_diaria", "cotizacion");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void creaTodasLasTablasConRlsActivado() {
        List<String> conRls = jdbc.queryForList("""
                SELECT relname FROM pg_class
                WHERE relnamespace = 'public'::regnamespace AND relkind = 'r' AND relrowsecurity
                """, String.class);

        assertThat(conRls).containsAll(TABLAS).contains("flyway_schema_history");
    }

    @Test
    void tieneIndiceEspacialSobreLaUbicacionDePublicaciones() {
        String definicion = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'publicacion_ubicacion_idx'", String.class);

        assertThat(definicion).contains("USING gist");
    }

    @Test
    void metricaDiariaNoDuplicaVistasDePerfilDelMismoDia() {
        UUID vendedorId = crearVendedor();
        String insert = """
                INSERT INTO metrica_diaria (fecha, vendedor_id, publicacion_id, tipo, cantidad)
                VALUES (current_date, ?, NULL, 'VISTA_PERFIL', 1)
                """;
        jdbc.update(insert, vendedorId);

        assertThatThrownBy(() -> jdbc.update(insert, vendedorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void metricaDiariaExigePublicacionSoloEnVistasDePublicacion() {
        UUID vendedorId = crearVendedor();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO metrica_diaria (fecha, vendedor_id, publicacion_id, tipo, cantidad)
                VALUES (current_date, ?, NULL, 'VISTA_PUBLICACION', 1)
                """, vendedorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID crearVendedor() {
        UUID usuarioId = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id, email) VALUES (?, ?)", usuarioId, "vendedor@example.com");
        return jdbc.queryForObject("""
                INSERT INTO vendedor (usuario_id, tipo, slug, nombre_publico, whatsapp, ubicacion)
                VALUES (?, 'CONCESIONARIA', 'autos-del-sur', 'Autos del Sur', '5491100000000',
                        ST_GeogFromText('POINT(-58.38 -34.60)'))
                RETURNING id
                """, UUID.class, usuarioId);
    }
}
