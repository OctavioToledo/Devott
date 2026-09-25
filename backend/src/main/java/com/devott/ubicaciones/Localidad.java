package com.devott.ubicaciones;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Localidad argentina con su centroide.
 */
@Schema(name = "Localidad")
public record Localidad(String id, String nombre, String provincia, double lat, double lng) {
}
