package com.devott.publicaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Precio en dólares de referencia, para filtrar y ordenar publicaciones en distintas monedas. */
final class PreciosUsd {

    private PreciosUsd() {
    }

    /**
     * @param dolar pesos por dólar; si es null, un precio en pesos queda sin referencia hasta que haya cotización.
     */
    static BigDecimal calcular(BigDecimal precio, Moneda moneda, BigDecimal dolar) {
        if (moneda == Moneda.USD) {
            return precio;
        }
        return dolar == null ? null : precio.divide(dolar, 2, RoundingMode.HALF_UP);
    }
}
