package com.devott.interacciones;

import com.devott.compartido.errores.ConflictoException;
import com.devott.usuarios.UsuarioService;
import com.devott.vendedores.TipoVendedor;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Vendedores que sigue un comprador. Las notificaciones a seguidores quedan fuera del MVP. */
@Service
@Transactional(readOnly = true)
public class SeguimientoService {

    @Schema(name = "VendedorSeguido")
    public record VendedorSeguido(String slug, String nombrePublico, TipoVendedor tipo, boolean verificado,
                                  String logoUrl, String ciudad, String provincia,
                                  @Schema(description = "Publicaciones activas") long autosActivos) {
    }

    private record Fila(UUID vendedorId, long autosActivos) {
    }

    private final VendedorService vendedores;
    private final UsuarioService usuarios;
    private final JdbcClient jdbc;

    SeguimientoService(VendedorService vendedores, UsuarioService usuarios, JdbcClient jdbc) {
        this.vendedores = vendedores;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    /** Sigue al vendedor. Si ya lo seguía no hace nada. No se puede seguir el perfil propio. */
    @Transactional
    public void seguir(Jwt jwt, String vendedorSlug) {
        Vendedor vendedor = vendedores.porSlug(vendedorSlug);
        UUID usuarioId = usuarios.sincronizar(jwt).getId();
        if (vendedor.getUsuarioId().equals(usuarioId)) {
            throw new ConflictoException("No podés seguir tu propio perfil.");
        }
        jdbc.sql("INSERT INTO seguimiento (usuario_id, vendedor_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params(usuarioId, vendedor.getId())
                .update();
    }

    /** Deja de seguir al vendedor. Si no lo seguía (o no existe) no hace nada. */
    @Transactional
    public void dejarDeSeguir(UUID usuarioId, String vendedorSlug) {
        jdbc.sql("""
                        DELETE FROM seguimiento s USING vendedor v
                        WHERE s.vendedor_id = v.id AND s.usuario_id = ? AND v.slug = ?
                        """)
                .params(usuarioId, vendedorSlug)
                .update();
    }

    public List<String> slugs(UUID usuarioId) {
        return jdbc.sql("""
                        SELECT v.slug FROM seguimiento s JOIN vendedor v ON v.id = s.vendedor_id
                        WHERE s.usuario_id = ? ORDER BY s.creado_en DESC
                        """)
                .param(usuarioId)
                .query(String.class)
                .list();
    }

    /** Vendedores seguidos, los últimos primero, con cuántos autos activos tienen. */
    public List<VendedorSeguido> listar(UUID usuarioId) {
        List<Fila> filas = jdbc.sql("""
                        SELECT s.vendedor_id,
                               (SELECT count(*) FROM publicacion p
                                WHERE p.vendedor_id = s.vendedor_id AND p.estado = 'ACTIVA') AS autos_activos
                        FROM seguimiento s
                        WHERE s.usuario_id = ?
                        ORDER BY s.creado_en DESC, s.vendedor_id
                        """)
                .param(usuarioId)
                .query((rs, i) -> new Fila(rs.getObject("vendedor_id", UUID.class), rs.getLong("autos_activos")))
                .list();
        return filas.stream()
                .map(f -> {
                    Vendedor v = vendedores.porId(f.vendedorId()).orElseThrow();
                    return new VendedorSeguido(v.getSlug(), v.getNombrePublico(), v.getTipo(), v.isVerificado(),
                            vendedores.urlDelLogo(v), v.getCiudad(), v.getProvincia(), f.autosActivos());
                })
                .toList();
    }
}
