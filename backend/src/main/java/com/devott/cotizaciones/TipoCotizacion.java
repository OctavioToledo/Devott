package com.devott.cotizaciones;

public enum TipoCotizacion {
    OFICIAL("oficial"),
    BLUE("blue"),
    MEP("bolsa");

    /** Nombre de la "casa" en dolarapi.com. */
    final String casa;

    TipoCotizacion(String casa) {
        this.casa = casa;
    }
}
