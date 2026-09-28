package com.devott.suscripciones;

import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.suscripciones.SuscripcionService.Suscripcion;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Planes", description = "Planes a la venta y el plan del vendedor logueado")
class SuscripcionesController {

    /** Qué mostrar en el panel sobre el plan. */
    enum Aviso {
        /** No tiene plan: no puede publicar. */
        SIN_PLAN,
        /** Vence en {@link SuscripcionService#DIAS_DE_AVISO} días o menos. */
        POR_VENCER,
        /** Ya venció: en los días de gracia, antes de que se pausen las publicaciones. */
        EN_GRACIA,
        /** Usa todas las publicaciones activas del plan. */
        LIMITE_ALCANZADO
    }

    @Schema(name = "MiPlan")
    record MiPlan(
            @Schema(description = "Null si no tiene plan") Plan plan,
            boolean esPrueba,
            LocalDate inicio,
            LocalDate venceEl,
            @Schema(description = "Si no se renueva, ese día se pausan las publicaciones") LocalDate pausaEl,
            @Schema(description = "Días hasta el vencimiento (negativo si ya venció)") Long diasParaVencer,
            long publicacionesActivas,
            int maxPublicaciones,
            int maxFotos,
            List<Aviso> avisos) {
    }

    private final SuscripcionService suscripciones;
    private final VendedorService vendedores;

    SuscripcionesController(SuscripcionService suscripciones, VendedorService vendedores) {
        this.suscripciones = suscripciones;
        this.vendedores = vendedores;
    }

    @GetMapping("/api/v1/planes")
    @Operation(summary = "Planes a la venta", description = "Del más chico al más grande. Precio mensual en pesos.")
    List<Plan> planes() {
        return suscripciones.planes();
    }

    @GetMapping("/api/v1/me/suscripcion")
    @Operation(summary = "Mi plan", description = "Plan vigente, vencimiento, uso y avisos para el panel.")
    @ApiResponse(responseCode = "404", description = "El usuario todavía no tiene perfil de vendedor")
    MiPlan miPlan(@AuthenticationPrincipal Jwt jwt) {
        Vendedor vendedor = vendedores.deUsuario(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Primero creá tu perfil de vendedor."));
        long activas = suscripciones.publicacionesActivas(vendedor.getId());
        Optional<Suscripcion> vigente = suscripciones.vigente(vendedor.getId());
        if (vigente.isEmpty()) {
            Limites sin = LimitesService.SIN_PLAN;
            return new MiPlan(null, false, null, null, null, null, activas, sin.maxPublicaciones(), sin.maxFotos(),
                    List.of(Aviso.SIN_PLAN));
        }
        Suscripcion s = vigente.get();
        long dias = suscripciones.diasHasta(s.venceEl());
        List<Aviso> avisos = new java.util.ArrayList<>();
        if (dias < 0) avisos.add(Aviso.EN_GRACIA);
        else if (dias <= SuscripcionService.DIAS_DE_AVISO) avisos.add(Aviso.POR_VENCER);
        if (activas >= s.plan().maxPublicaciones()) avisos.add(Aviso.LIMITE_ALCANZADO);
        return new MiPlan(s.plan(), s.esPrueba(), s.inicio(), s.venceEl(), s.finDeGracia().plusDays(1), dias, activas,
                s.plan().maxPublicaciones(), s.plan().maxFotos(), avisos);
    }
}
