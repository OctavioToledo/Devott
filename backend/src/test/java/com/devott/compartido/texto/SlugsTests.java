package com.devott.compartido.texto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTests {

    @Test
    void generaSlugsSinAcentosNiSimbolos() {
        assertThat(Slugs.generar("Automotores del Sur")).isEqualTo("automotores-del-sur");
        assertThat(Slugs.generar("  Peña & Hnos. S.R.L. ")).isEqualTo("pena-hnos-s-r-l");
        assertThat(Slugs.generar("Citroën 2008!!")).isEqualTo("citroen-2008");
    }

    @Test
    void validaElFormato() {
        assertThat(Slugs.esValido("autos-del-sur")).isTrue();
        assertThat(Slugs.esValido("Autos")).isFalse();
        assertThat(Slugs.esValido("autos--sur")).isFalse();
        assertThat(Slugs.esValido("-autos")).isFalse();
        assertThat(Slugs.esValido(null)).isFalse();
    }
}
