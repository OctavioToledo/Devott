package com.devott.metricas;

import com.devott.compartido.errores.DatosInvalidosException;
import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Métricas del vendedor: vistas, clics a WhatsApp y guardados, por día y por publicación. */
@Service
@Transactional(readOnly = true)
public class MetricaService {

    static final Set<Integer> PERIODOS = Set.of(7, 30, 90);

    @Schema(name = "TotalesMetricas")
    public record Totales(long vistasPerfil, long vistasPublicaciones, long contactos, long guardados) {

        Totales mas(Dia d) {
            return new Totales(vistasPerfil + d.vistasPerfil(), vistasPublicaciones + d.vistasPublicaciones(),
                    contactos + d.contactos(), guardados + d.guardados());
        }
    }

    @Schema(name = "MetricasDelDia")
    public record Dia(LocalDate fecha, long vistasPerfil, long vistasPublicaciones, long contactos, long guardados) {
    }

    @Schema(name = "MetricasDePublicacion")
    public record DePublicacion(UUID id, long vistas, long contactos, long guardados) {
    }

    @Schema(name = "Metricas")
    public record Metricas(
            int dias,
            LocalDate desde,
            LocalDate hasta,
            @Schema(description = "Totales del período") Totales actual,
            @Schema(description = "Totales de los días inmediatamente anteriores, del mismo largo") Totales anterior,
            @Schema(description = "Un elemento por día del período, también los días sin actividad") List<Dia> serie,
            @Schema(description = "Solo publicaciones con actividad en el período") List<DePublicacion> publicaciones) {
    }

    private final VendedorService vendedores;
    private final JdbcClient jdbc;
    private final Clock reloj;

    MetricaService(VendedorService vendedores, JdbcClient jdbc, Clock reloj) {
        this.vendedores = vendedores;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    public Metricas deUsuario(UUID usuarioId, int dias) {
        if (!PERIODOS.contains(dias)) {
            throw new DatosInvalidosException("dias", "Elegí 7, 30 o 90 días.");
        }
        Vendedor vendedor = vendedores.deUsuario(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Primero creá tu perfil de vendedor."));

        LocalDate hasta = LocalDate.now(reloj.withZone(VistaService.ARGENTINA));
        LocalDate desde = hasta.minusDays(dias - 1L);
        LocalDate desdeAnterior = desde.minusDays(dias);

        List<Dia> todos = porDia(vendedor.getId(), desdeAnterior, hasta);
        Totales actual = new Totales(0, 0, 0, 0);
        Totales anterior = new Totales(0, 0, 0, 0);
        for (Dia d : todos) {
            if (d.fecha().isBefore(desde)) anterior = anterior.mas(d);
            else actual = actual.mas(d);
        }
        List<Dia> serie = todos.stream().filter(d -> !d.fecha().isBefore(desde)).toList();
        return new Metricas(dias, desde, hasta, actual, anterior, serie, porPublicacion(vendedor.getId(), desde, hasta));
    }

    private List<Dia> porDia(UUID vendedorId, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                        WITH dias AS (
                            SELECT d::date AS fecha FROM generate_series(:desde::date, :hasta::date, interval '1 day') d
                        ),
                        vistas AS (
                            SELECT fecha,
                                   sum(cantidad) FILTER (WHERE tipo = 'VISTA_PERFIL') AS perfil,
                                   sum(cantidad) FILTER (WHERE tipo = 'VISTA_PUBLICACION') AS publicaciones
                            FROM metrica_diaria
                            WHERE vendedor_id = :vendedor AND fecha BETWEEN :desde AND :hasta
                            GROUP BY fecha
                        ),
                        contactos AS (
                            SELECT (creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date AS fecha, count(*) AS n
                            FROM contacto
                            WHERE vendedor_id = :vendedor
                              AND (creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date BETWEEN :desde AND :hasta
                            GROUP BY 1
                        ),
                        guardados AS (
                            SELECT (g.creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date AS fecha, count(*) AS n
                            FROM guardado g JOIN publicacion p ON p.id = g.publicacion_id
                            WHERE p.vendedor_id = :vendedor
                              AND (g.creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date BETWEEN :desde AND :hasta
                            GROUP BY 1
                        )
                        SELECT d.fecha,
                               coalesce(v.perfil, 0) AS vistas_perfil,
                               coalesce(v.publicaciones, 0) AS vistas_publicaciones,
                               coalesce(c.n, 0) AS contactos,
                               coalesce(g.n, 0) AS guardados
                        FROM dias d
                        LEFT JOIN vistas v ON v.fecha = d.fecha
                        LEFT JOIN contactos c ON c.fecha = d.fecha
                        LEFT JOIN guardados g ON g.fecha = d.fecha
                        ORDER BY d.fecha
                        """)
                .param("vendedor", vendedorId)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, i) -> new Dia(rs.getObject("fecha", LocalDate.class), rs.getLong("vistas_perfil"),
                        rs.getLong("vistas_publicaciones"), rs.getLong("contactos"), rs.getLong("guardados")))
                .list();
    }

    private List<DePublicacion> porPublicacion(UUID vendedorId, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                        SELECT p.id,
                               (SELECT coalesce(sum(m.cantidad), 0) FROM metrica_diaria m
                                WHERE m.publicacion_id = p.id AND m.fecha BETWEEN :desde AND :hasta) AS vistas,
                               (SELECT count(*) FROM contacto c
                                WHERE c.publicacion_id = p.id
                                  AND (c.creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date BETWEEN :desde AND :hasta) AS contactos,
                               (SELECT count(*) FROM guardado g
                                WHERE g.publicacion_id = p.id
                                  AND (g.creado_en AT TIME ZONE 'America/Argentina/Buenos_Aires')::date BETWEEN :desde AND :hasta) AS guardados
                        FROM publicacion p
                        WHERE p.vendedor_id = :vendedor
                        """)
                .param("vendedor", vendedorId)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, i) -> new DePublicacion(rs.getObject("id", UUID.class), rs.getLong("vistas"),
                        rs.getLong("contactos"), rs.getLong("guardados")))
                .list()
                .stream()
                .filter(p -> p.vistas() + p.contactos() + p.guardados() > 0)
                .toList();
    }
}
