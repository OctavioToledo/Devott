package com.devott.publicaciones;

public enum OrdenBusqueda {
    /** Publicadas más recientemente primero. */
    RECIENTES,
    PRECIO_ASC,
    PRECIO_DESC,
    KM_ASC,
    /** Modelos más nuevos primero. */
    ANIO_DESC,
    /** Más cercanas primero. Solo si se busca por zona. */
    CERCANIA
}
