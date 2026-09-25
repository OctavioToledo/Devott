package com.devott.publicaciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CambioEstadoRequest")
record CambioEstadoRequest(@NotNull(message = "Indicá el estado.") EstadoPublicacion estado) {
}
