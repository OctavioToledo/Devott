package com.devott.catalogo;

/**
 * Modelo con los datos de su marca, para armar títulos ("Toyota Hilux") y slugs.
 */
public record ModeloConMarca(Integer modeloId, String modelo, String modeloSlug,
                             Integer marcaId, String marca, String marcaSlug) {

    public String titulo() {
        return marca + " " + modelo;
    }
}
