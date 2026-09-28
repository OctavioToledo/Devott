package com.devott.metricas;

import com.devott.metricas.MetricaService.Metricas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Métricas", description = "Métricas del vendedor logueado")
class MisMetricasController {

    private final MetricaService metricas;

    MisMetricasController(MetricaService metricas) {
        this.metricas = metricas;
    }

    @GetMapping("/api/v1/me/metricas")
    @Operation(summary = "Mis métricas",
            description = "Visitas al perfil, vistas de publicaciones, clics a WhatsApp y guardados nuevos del "
                    + "período (hasta hoy, en hora argentina), comparados con el período anterior del mismo largo.")
    @ApiResponse(responseCode = "404", description = "El usuario todavía no tiene perfil de vendedor")
    Metricas metricas(@AuthenticationPrincipal Jwt jwt,
                      @Parameter(description = "7, 30 o 90") @RequestParam(defaultValue = "7") int dias) {
        return metricas.deUsuario(UUID.fromString(jwt.getSubject()), dias);
    }
}
