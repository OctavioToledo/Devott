package com.devott.publicaciones;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

final class FotoRequests {

    private FotoRequests() {
    }

    @Schema(name = "PedidoSubidaFoto")
    record PedidoSubida(
            @Schema(description = "image/webp o image/jpeg", example = "image/webp")
            @NotBlank(message = "Indicá el tipo de imagen.") String contentType) {
    }

    @Schema(name = "ConfirmacionFoto")
    record Confirmacion(
            @Schema(description = "La ruta que devolvió url-subida")
            @NotBlank(message = "Indicá la foto subida.") String ruta,
            @Min(1) @Max(10000) Integer ancho,
            @Min(1) @Max(10000) Integer alto) {
    }

    @Schema(name = "OrdenFotos")
    record Orden(@NotEmpty(message = "Indicá el orden de las fotos.") List<UUID> fotoIds) {
    }
}
