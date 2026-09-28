package com.devott.interacciones;

import com.devott.compartido.web.Pagina;
import com.devott.publicaciones.PublicacionResponses.Tarjeta;
import com.devott.publicaciones.PublicacionService;
import com.devott.usuarios.UsuarioService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Publicaciones guardadas (el corazón). Solo cuentan las visibles en público: si una se pausa,
 * deja de aparecer, y vuelve si se reactiva.
 */
@Service
@Transactional(readOnly = true)
public class GuardadoService {

    private static final String VISIBLE = "p.estado IN ('ACTIVA', 'VENDIDA')";

    private final PublicacionService publicaciones;
    private final UsuarioService usuarios;
    private final JdbcClient jdbc;

    GuardadoService(PublicacionService publicaciones, UsuarioService usuarios, JdbcClient jdbc) {
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    /** Guarda la publicación. Si ya estaba guardada no hace nada. */
    @Transactional
    public void guardar(Jwt jwt, String slug) {
        UUID publicacionId = publicaciones.idVisible(slug);
        UUID usuarioId = usuarios.sincronizar(jwt).getId();
        jdbc.sql("INSERT INTO guardado (usuario_id, publicacion_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params(usuarioId, publicacionId)
                .update();
    }

    /** Quita la publicación de guardados. Si no estaba guardada (o no existe) no hace nada. */
    @Transactional
    public void quitar(UUID usuarioId, String slug) {
        jdbc.sql("""
                        DELETE FROM guardado g USING publicacion p
                        WHERE g.publicacion_id = p.id AND g.usuario_id = ? AND p.slug = ?
                        """)
                .params(usuarioId, slug)
                .update();
    }

    /** Slugs guardados y visibles, para marcar los corazones. */
    public List<String> slugs(UUID usuarioId) {
        return jdbc.sql("""
                        SELECT p.slug FROM guardado g JOIN publicacion p ON p.id = g.publicacion_id
                        WHERE g.usuario_id = ? AND %s
                        ORDER BY g.creado_en DESC
                        """.formatted(VISIBLE))
                .param(usuarioId)
                .query(String.class)
                .list();
    }

    /** Guardados visibles, los últimos guardados primero. */
    public Pagina<Tarjeta> listar(UUID usuarioId, int pagina, int tamano) {
        long total = jdbc.sql("""
                        SELECT count(*) FROM guardado g JOIN publicacion p ON p.id = g.publicacion_id
                        WHERE g.usuario_id = ? AND %s
                        """.formatted(VISIBLE))
                .param(usuarioId)
                .query(Long.class)
                .single();
        List<UUID> ids = jdbc.sql("""
                        SELECT p.id FROM guardado g JOIN publicacion p ON p.id = g.publicacion_id
                        WHERE g.usuario_id = ? AND %s
                        ORDER BY g.creado_en DESC, p.id
                        LIMIT ? OFFSET ?
                        """.formatted(VISIBLE))
                .params(usuarioId, tamano, (long) pagina * tamano)
                .query(UUID.class)
                .list();
        List<Tarjeta> items = publicaciones.tarjetasVisibles(ids);
        long hasta = (long) pagina * tamano + items.size();
        return new Pagina<>(items, pagina, tamano, total, hasta < total);
    }
}
