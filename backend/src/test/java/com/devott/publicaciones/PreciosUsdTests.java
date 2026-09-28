package com.devott.publicaciones;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PreciosUsdTests {

    @Test
    void enDolaresEsElMismoPrecio() {
        assertThat(PreciosUsd.calcular(new BigDecimal("34900"), Moneda.USD, new BigDecimal("1565")))
                .isEqualByComparingTo("34900");
    }

    @Test
    void enPesosDivideYRedondeaADosDecimales() {
        assertThat(PreciosUsd.calcular(new BigDecimal("25000000"), Moneda.ARS, new BigDecimal("1565")))
                .isEqualTo(new BigDecimal("15974.44"));
    }

    @Test
    void enPesosSinCotizacionQuedaSinReferencia() {
        assertThat(PreciosUsd.calcular(new BigDecimal("25000000"), Moneda.ARS, null)).isNull();
    }
}
