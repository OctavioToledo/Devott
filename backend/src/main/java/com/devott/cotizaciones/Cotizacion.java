package com.devott.cotizaciones;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valor de venta de un tipo de dólar, en pesos, en una fecha. */
public record Cotizacion(TipoCotizacion tipo, LocalDate fecha, BigDecimal valor) {
}
