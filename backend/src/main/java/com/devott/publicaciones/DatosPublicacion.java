package com.devott.publicaciones;

import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

/**
 * Datos de una publicación ya validados y normalizados, listos para guardar.
 */
record DatosPublicacion(
        Integer modeloId,
        String version,
        int anio,
        int km,
        Condicion condicion,
        BigDecimal precio,
        Moneda moneda,
        BigDecimal precioUsdRef,
        Carroceria carroceria,
        Combustible combustible,
        Transmision transmision,
        Traccion traccion,
        String color,
        Integer puertas,
        boolean financia,
        boolean aceptaPermuta,
        boolean unicoDueno,
        String descripcion,
        String ciudad,
        String provincia,
        Point ubicacion) {
}
