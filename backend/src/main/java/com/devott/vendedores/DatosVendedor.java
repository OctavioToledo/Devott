package com.devott.vendedores;

import org.locationtech.jts.geom.Point;

/**
 * Datos del perfil ya validados y normalizados, listos para guardar.
 */
record DatosVendedor(
        TipoVendedor tipo,
        String slug,
        String nombrePublico,
        String whatsapp,
        String telefono,
        String descripcion,
        String direccion,
        String ciudad,
        String provincia,
        Point ubicacion,
        String horarios,
        String instagram,
        String facebook) {
}
