package com.devott.suscripciones;

import com.devott.compartido.errores.DatosInvalidosException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Planes y suscripciones con cobro manual. Una suscripción está vigente mientras está activa y no pasaron
 * más de {@link #DIAS_DE_GRACIA} días de su vencimiento; después vence y se pausan las publicaciones.
 */
@Service
@Transactional(readOnly = true)
public class SuscripcionService {

    public static final int DIAS_DE_GRACIA = 7;
    /** Desde cuántos días antes del vencimiento se avisa en el panel. */
    public static final int DIAS_DE_AVISO = 7;
    static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private static final Logger log = LoggerFactory.getLogger(SuscripcionService.class);

    @Schema(name = "Suscripcion")
    public record Suscripcion(UUID id, UUID vendedorId, Plan plan, boolean esPrueba, LocalDate inicio,
                              LocalDate venceEl, String referencia) {

        /** Último día antes de que se pausen las publicaciones. */
        public LocalDate finDeGracia() {
            return venceEl.plusDays(DIAS_DE_GRACIA);
        }
    }

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    SuscripcionService(JdbcClient jdbc, ApplicationEventPublisher eventos, Clock reloj) {
        this.jdbc = jdbc;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj.withZone(ARGENTINA));
    }

    /** Planes a la venta, del más chico al más grande. */
    public List<Plan> planes() {
        return jdbc.sql("""
                        SELECT codigo, nombre, max_publicaciones, max_fotos, precio_ars FROM plan
                        WHERE activo ORDER BY orden, precio_ars
                        """)
                .query((rs, i) -> new Plan(rs.getString("codigo"), rs.getString("nombre"),
                        rs.getInt("max_publicaciones"), rs.getInt("max_fotos"), rs.getBigDecimal("precio_ars")))
                .list();
    }

    /** Suscripción vigente (incluye los días de gracia), si tiene. */
    public Optional<Suscripcion> vigente(UUID vendedorId) {
        return jdbc.sql(SELECT_SUSCRIPCION + " WHERE s.vendedor_id = ? AND s.estado = 'ACTIVA' AND s.vence_el >= ?")
                .params(vendedorId, hoy().minusDays(DIAS_DE_GRACIA))
                .query(SuscripcionService::suscripcion)
                .optional();
    }

    /**
     * Asigna un plan desde la administración: reemplaza la suscripción activa (si hay) por una nueva
     * que empieza hoy. Sirve para dar de alta, renovar, cambiar de plan o dar una prueba gratis.
     */
    @Transactional
    public Suscripcion asignar(UUID vendedorId, String codigoPlan, LocalDate venceEl, boolean esPrueba, String referencia) {
        LocalDate hoy = hoy();
        if (venceEl.isBefore(hoy)) {
            throw new DatosInvalidosException("venceEl", "La fecha de vencimiento no puede ser anterior a hoy.");
        }
        UUID planId = jdbc.sql("SELECT id FROM plan WHERE codigo = ? AND activo")
                .param(codigoPlan)
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new DatosInvalidosException("plan", "No existe ese plan."));

        jdbc.sql("UPDATE suscripcion SET estado = 'CANCELADA' WHERE vendedor_id = ? AND estado = 'ACTIVA'")
                .param(vendedorId)
                .update();
        UUID id = jdbc.sql("""
                        INSERT INTO suscripcion (vendedor_id, plan_id, estado, inicio, vence_el, es_prueba, proveedor_pago_ref)
                        VALUES (?, ?, 'ACTIVA', ?, ?, ?, ?)
                        RETURNING id
                        """)
                .params(vendedorId, planId, hoy, venceEl, esPrueba, referencia)
                .query(UUID.class)
                .single();
        return jdbc.sql(SELECT_SUSCRIPCION + " WHERE s.id = ?").param(id).query(SuscripcionService::suscripcion).single();
    }

    /** Cancela la suscripción activa ya mismo y pausa las publicaciones. Si no tenía, no hace nada. */
    @Transactional
    public void cancelar(UUID vendedorId) {
        int canceladas = jdbc.sql("UPDATE suscripcion SET estado = 'CANCELADA' WHERE vendedor_id = ? AND estado = 'ACTIVA'")
                .param(vendedorId)
                .update();
        if (canceladas > 0) {
            eventos.publishEvent(new SuscripcionTerminada(vendedorId));
        }
    }

    /** Vence las suscripciones que pasaron la gracia y pausa sus publicaciones. Devuelve cuántas venció. */
    @Scheduled(cron = "${devott.suscripciones.vencimiento-cron}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public int vencerAtrasadas() {
        List<UUID> vendedores = jdbc.sql("""
                        UPDATE suscripcion SET estado = 'VENCIDA'
                        WHERE estado = 'ACTIVA' AND vence_el < ?
                        RETURNING vendedor_id
                        """)
                .param(hoy().minusDays(DIAS_DE_GRACIA))
                .query(UUID.class)
                .list();
        vendedores.forEach(v -> eventos.publishEvent(new SuscripcionTerminada(v)));
        if (!vendedores.isEmpty()) {
            log.info("Vencieron {} suscripciones y se pausaron sus publicaciones", vendedores.size());
        }
        return vendedores.size();
    }

    /** Suscripciones activas que vencen en los próximos `dias` o ya están en gracia, las más urgentes primero. */
    public List<Suscripcion> porVencer(int dias) {
        return jdbc.sql(SELECT_SUSCRIPCION + " WHERE s.estado = 'ACTIVA' AND s.vence_el <= ? ORDER BY s.vence_el")
                .param(hoy().plusDays(dias))
                .query(SuscripcionService::suscripcion)
                .list();
    }

    public long publicacionesActivas(UUID vendedorId) {
        return jdbc.sql("SELECT count(*) FROM publicacion WHERE vendedor_id = ? AND estado = 'ACTIVA'")
                .param(vendedorId)
                .query(Long.class)
                .single();
    }

    long diasHasta(LocalDate fecha) {
        return ChronoUnit.DAYS.between(hoy(), fecha);
    }

    private static final String SELECT_SUSCRIPCION = """
            SELECT s.id, s.vendedor_id, s.es_prueba, s.inicio, s.vence_el, s.proveedor_pago_ref,
                   p.codigo, p.nombre, p.max_publicaciones, p.max_fotos, p.precio_ars
            FROM suscripcion s JOIN plan p ON p.id = s.plan_id
            """;

    private static Suscripcion suscripcion(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        Plan plan = new Plan(rs.getString("codigo"), rs.getString("nombre"), rs.getInt("max_publicaciones"),
                rs.getInt("max_fotos"), rs.getBigDecimal("precio_ars"));
        return new Suscripcion(rs.getObject("id", UUID.class), rs.getObject("vendedor_id", UUID.class), plan,
                rs.getBoolean("es_prueba"), rs.getObject("inicio", LocalDate.class),
                rs.getObject("vence_el", LocalDate.class), rs.getString("proveedor_pago_ref"));
    }
}
