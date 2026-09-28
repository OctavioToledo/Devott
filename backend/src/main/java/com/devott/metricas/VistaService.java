package com.devott.metricas;

import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.publicaciones.PublicacionService;
import com.devott.publicaciones.PublicacionService.ParaContacto;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Cuenta vistas de perfiles y publicaciones: una por visitante, por objeto y por día (hora argentina).
 */
@Service
public class VistaService {

    static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final VendedorService vendedores;
    private final PublicacionService publicaciones;
    private final JdbcClient jdbc;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    VistaService(VendedorService vendedores, PublicacionService publicaciones, JdbcClient jdbc,
                 PlatformTransactionManager transacciones, Clock reloj) {
        this.vendedores = vendedores;
        this.publicaciones = publicaciones;
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(transacciones);
        this.reloj = reloj;
    }

    /**
     * Registra la vista si es la primera del visitante en el día. Si el perfil no existe o la publicación
     * no está activa, no hace nada. Devuelve true si la vista se contó. La búsqueda va fuera de la
     * transacción: un 404 de los otros servicios la marcaría para rollback aunque se capture.
     */
    public boolean registrar(TipoMetrica tipo, String slug, String visitante) {
        UUID vendedorId;
        UUID publicacionId = null;
        try {
            if (tipo == TipoMetrica.VISTA_PERFIL) {
                Vendedor vendedor = vendedores.porSlug(slug);
                vendedorId = vendedor.getId();
            } else {
                ParaContacto publicacion = publicaciones.paraContacto(slug);
                vendedorId = publicacion.vendedorId();
                publicacionId = publicacion.publicacionId();
            }
        } catch (RecursoNoEncontradoException e) {
            return false;
        }

        UUID vendedor = vendedorId;
        UUID publicacion = publicacionId;
        return Boolean.TRUE.equals(transaccion.execute(estado -> contar(tipo, vendedor, publicacion, visitante)));
    }

    private boolean contar(TipoMetrica tipo, UUID vendedorId, UUID publicacionId, String visitante) {
        LocalDate hoy = LocalDate.now(reloj.withZone(ARGENTINA));
        int nueva = jdbc.sql("""
                        INSERT INTO vista_registrada (fecha, visitante, tipo, objeto_id) VALUES (?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                        """)
                .params(hoy, hash(visitante), tipo.name(), publicacionId != null ? publicacionId : vendedorId)
                .update();
        if (nueva == 0) {
            return false;
        }
        jdbc.sql("""
                        INSERT INTO metrica_diaria (fecha, vendedor_id, publicacion_id, tipo, cantidad)
                        VALUES (?, ?, ?, ?, 1)
                        ON CONFLICT ON CONSTRAINT metrica_diaria_clave_key
                        DO UPDATE SET cantidad = metrica_diaria.cantidad + 1
                        """)
                .params(hoy, vendedorId, publicacionId, tipo.name())
                .update();
        return true;
    }

    /** Las vistas registradas solo sirven para deduplicar el mismo día: se borran las viejas. */
    @Scheduled(cron = "${devott.metricas.purga-cron:0 30 4 * * *}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public int purgarVistasViejas() {
        LocalDate limite = LocalDate.now(reloj.withZone(ARGENTINA)).minusDays(2);
        return jdbc.sql("DELETE FROM vista_registrada WHERE fecha < ?").param(limite).update();
    }

    /** No se guarda el id del navegador tal cual. */
    static String hash(String visitante) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(visitante.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
