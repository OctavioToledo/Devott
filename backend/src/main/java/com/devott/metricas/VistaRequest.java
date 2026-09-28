package com.devott.metricas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "VistaRequest")
record VistaRequest(
        @NotNull TipoMetrica tipo,
        @Schema(description = "Slug del vendedor (VISTA_PERFIL) o de la publicación (VISTA_PUBLICACION)")
        @NotBlank @Size(max = 120) String slug,
        @Schema(description = "Id anónimo y al azar que genera el navegador y guarda para las próximas visitas")
        @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = "[A-Za-z0-9-]+") String visitante) {
}
