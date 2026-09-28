package com.devott.suscripciones;

import com.devott.compartido.seguridad.Administradores;
import com.devott.suscripciones.SuscripcionService.Suscripcion;
import com.devott.vendedores.TipoVendedor;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Administración de planes con cobro manual. Solo para los emails de DEVOTT_ADMINS. */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administración", description = "Asignar planes y pruebas gratis, ver vencimientos. Solo administradores.")
class AdminController {

    @Schema(name = "SuscripcionAdmin")
    record SuscripcionAdmin(String plan, String nombrePlan, boolean esPrueba, LocalDate inicio, LocalDate venceEl,
                            String referencia, boolean enGracia) {
    }

    @Schema(name = "VendedorAdmin")
    record VendedorAdmin(String slug, String nombrePublico, TipoVendedor tipo, String ciudad, String provincia,
                         String email, long publicacionesActivas, SuscripcionAdmin suscripcion) {
    }

    @Schema(name = "AsignarPlanRequest")
    record AsignarPlanRequest(
            @Schema(description = "Código del plan, por ejemplo CONCESIONARIA") @NotBlank String plan,
            @Schema(description = "Último día pago (o de la prueba)") @NotNull LocalDate venceEl,
            @Schema(description = "Prueba gratis") boolean esPrueba,
            @Schema(description = "Comprobante o nota del pago") @Size(max = 200) String referencia) {
    }

    private final Administradores administradores;
    private final SuscripcionService suscripciones;
    private final VendedorService vendedores;
    private final JdbcClient jdbc;

    AdminController(Administradores administradores, SuscripcionService suscripciones, VendedorService vendedores,
                    JdbcClient jdbc) {
        this.administradores = administradores;
        this.suscripciones = suscripciones;
        this.vendedores = vendedores;
        this.jdbc = jdbc;
    }

    @GetMapping("/vendedores")
    @Operation(summary = "Busca vendedores", description = "Por nombre, link o email. Sin texto, los últimos 30 creados.")
    List<VendedorAdmin> buscar(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "") String q) {
        administradores.exigir(jwt);
        String patron = "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        List<UUID> ids = jdbc.sql("""
                        SELECT v.id FROM vendedor v JOIN usuario u ON u.id = v.usuario_id
                        WHERE lower(v.nombre_publico) LIKE :p OR v.slug LIKE :p OR lower(coalesce(u.email, '')) LIKE :p
                        ORDER BY v.creado_en DESC
                        LIMIT 30
                        """)
                .param("p", patron)
                .query(UUID.class)
                .list();
        return ids.stream().map(this::vendedorAdmin).toList();
    }

    @GetMapping("/suscripciones/por-vencer")
    @Operation(summary = "Suscripciones por vencer", description = "Las que vencen en los próximos días o ya están en gracia.")
    List<VendedorAdmin> porVencer(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(defaultValue = "14") @Min(0) @Max(90) int dias) {
        administradores.exigir(jwt);
        return suscripciones.porVencer(dias).stream().map(s -> vendedorAdmin(s.vendedorId())).toList();
    }

    @PutMapping("/vendedores/{slug}/suscripcion")
    @Operation(summary = "Asigna un plan", description = "Reemplaza el plan actual por uno nuevo desde hoy. "
            + "Sirve para dar de alta, renovar, cambiar de plan o dar una prueba gratis.")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    VendedorAdmin asignar(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug,
                          @Valid @RequestBody AsignarPlanRequest request) {
        administradores.exigir(jwt);
        Vendedor vendedor = vendedores.porSlug(slug);
        String referencia = request.referencia() == null || request.referencia().isBlank() ? null : request.referencia().trim();
        suscripciones.asignar(vendedor.getId(), request.plan(), request.venceEl(), request.esPrueba(), referencia);
        return vendedorAdmin(vendedor.getId());
    }

    @DeleteMapping("/vendedores/{slug}/suscripcion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela el plan", description = "Queda sin plan y se pausan sus publicaciones activas.")
    void cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        administradores.exigir(jwt);
        suscripciones.cancelar(vendedores.porSlug(slug).getId());
    }

    private VendedorAdmin vendedorAdmin(UUID vendedorId) {
        Vendedor v = vendedores.porId(vendedorId).orElseThrow();
        String email = jdbc.sql("SELECT email FROM usuario WHERE id = ?").param(v.getUsuarioId())
                .query(String.class).optional().orElse(null);
        SuscripcionAdmin suscripcion = suscripciones.vigente(vendedorId)
                .map(this::suscripcionAdmin)
                .orElse(null);
        return new VendedorAdmin(v.getSlug(), v.getNombrePublico(), v.getTipo(), v.getCiudad(), v.getProvincia(), email,
                suscripciones.publicacionesActivas(vendedorId), suscripcion);
    }

    private SuscripcionAdmin suscripcionAdmin(Suscripcion s) {
        return new SuscripcionAdmin(s.plan().codigo(), s.plan().nombre(), s.esPrueba(), s.inicio(), s.venceEl(),
                s.referencia(), suscripciones.diasHasta(s.venceEl()) < 0);
    }
}
