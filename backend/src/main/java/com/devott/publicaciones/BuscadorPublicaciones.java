package com.devott.publicaciones;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Arma y ejecuta la consulta del feed: solo publicaciones activas, con los filtros que vengan.
 * Devuelve ids en orden; los datos completos se cargan aparte.
 */
@Component
class BuscadorPublicaciones {

    record Coincidencia(UUID id, Double distanciaKm) {
    }

    record Resultado(List<Coincidencia> coincidencias, long total) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    BuscadorPublicaciones(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param precioMinUsd precio mínimo ya convertido a dólares (o null)
     * @param precioMaxUsd precio máximo ya convertido a dólares (o null)
     */
    Resultado buscar(FiltrosBusqueda f, BigDecimal precioMinUsd, BigDecimal precioMaxUsd) {
        List<String> condiciones = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        condiciones.add("p.estado = 'ACTIVA'");

        String distancia = "NULL::double precision";
        if (f.porZona()) {
            params.addValue("lat", f.lat()).addValue("lng", f.lng()).addValue("metros", f.radioKm() * 1000.0);
            String centro = "ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography";
            condiciones.add("ST_DWithin(p.ubicacion, " + centro + ", :metros)");
            distancia = "ST_Distance(p.ubicacion, " + centro + ") / 1000";
        }
        if (f.condicion() != null) {
            condiciones.add("p.condicion = :condicion");
            params.addValue("condicion", f.condicion().codigo());
        }
        if (f.tipoVendedor() != null) {
            condiciones.add("v.tipo = :tipoVendedor");
            params.addValue("tipoVendedor", f.tipoVendedor().name());
        }
        if (f.marca() != null) {
            condiciones.add("ma.slug = :marca");
            params.addValue("marca", f.marca());
            if (f.modelo() != null) {
                condiciones.add("mo.slug = :modelo");
                params.addValue("modelo", f.modelo());
            }
        }
        if (precioMinUsd != null) {
            condiciones.add("p.precio_usd_ref >= :precioMin");
            params.addValue("precioMin", precioMinUsd);
        }
        if (precioMaxUsd != null) {
            condiciones.add("p.precio_usd_ref <= :precioMax");
            params.addValue("precioMax", precioMaxUsd);
        }
        if (f.anioMin() != null) {
            condiciones.add("p.anio >= :anioMin");
            params.addValue("anioMin", f.anioMin());
        }
        if (f.anioMax() != null) {
            condiciones.add("p.anio <= :anioMax");
            params.addValue("anioMax", f.anioMax());
        }
        if (f.kmMax() != null) {
            condiciones.add("p.km <= :kmMax");
            params.addValue("kmMax", f.kmMax());
        }
        if (!f.carroceria().isEmpty()) {
            condiciones.add("p.carroceria IN (:carroceria)");
            params.addValue("carroceria", f.carroceria().stream().map(Enum::name).toList());
        }
        if (!f.combustible().isEmpty()) {
            condiciones.add("p.combustible IN (:combustible)");
            params.addValue("combustible", f.combustible().stream().map(Enum::name).toList());
        }
        if (!f.transmision().isEmpty()) {
            condiciones.add("p.transmision IN (:transmision)");
            params.addValue("transmision", f.transmision().stream().map(Enum::name).toList());
        }
        if (Boolean.TRUE.equals(f.financia())) {
            condiciones.add("p.financia");
        }
        if (Boolean.TRUE.equals(f.permuta())) {
            condiciones.add("p.acepta_permuta");
        }
        if (Boolean.TRUE.equals(f.unicoDueno())) {
            condiciones.add("p.unico_dueno");
        }

        params.addValue("limite", f.tamano()).addValue("desplazamiento", (long) f.pagina() * f.tamano());
        String sql = """
                SELECT p.id, %s AS distancia_km, count(*) OVER () AS total
                FROM publicacion p
                JOIN vendedor v ON v.id = p.vendedor_id
                JOIN modelo mo ON mo.id = p.modelo_id
                JOIN marca ma ON ma.id = mo.marca_id
                WHERE %s
                ORDER BY %s
                LIMIT :limite OFFSET :desplazamiento
                """.formatted(distancia, String.join("\n  AND ", condiciones), orden(f.orden()));

        long[] total = {0};
        List<Coincidencia> coincidencias = jdbc.query(sql, params, (rs, i) -> {
            total[0] = rs.getLong("total");
            return new Coincidencia(rs.getObject("id", UUID.class), rs.getObject("distancia_km", Double.class));
        });
        if (coincidencias.isEmpty() && f.pagina() > 0) {
            // Página fuera de rango: el total sale de contar sin paginar.
            total[0] = contar(sql, params);
        }
        return new Resultado(coincidencias, total[0]);
    }

    private long contar(String sql, MapSqlParameterSource params) {
        String sinPaginar = sql.substring(0, sql.indexOf("ORDER BY"));
        Long total = jdbc.queryForObject("SELECT count(*) FROM (" + sinPaginar + ") t", params, Long.class);
        return total == null ? 0 : total;
    }

    /** Siempre desempata por publicada_en e id, para que la paginación sea estable. */
    private static String orden(OrdenBusqueda orden) {
        String criterio = switch (orden) {
            case RECIENTES -> "";
            case PRECIO_ASC -> "p.precio_usd_ref ASC NULLS LAST, ";
            case PRECIO_DESC -> "p.precio_usd_ref DESC NULLS LAST, ";
            case KM_ASC -> "p.km ASC, ";
            case ANIO_DESC -> "p.anio DESC, ";
            case CERCANIA -> "distancia_km ASC NULLS LAST, ";
        };
        return criterio + "p.publicada_en DESC NULLS LAST, p.id";
    }
}
