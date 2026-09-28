package com.devott.cotizaciones;

/** Fuente externa de cotizaciones. */
public interface ProveedorDeCotizaciones {

    Cotizacion actual(TipoCotizacion tipo);
}
