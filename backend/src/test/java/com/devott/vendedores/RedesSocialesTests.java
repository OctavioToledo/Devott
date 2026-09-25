package com.devott.vendedores;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedesSocialesTests {

    @Test
    void extraeElUsuarioDeInstagram() {
        assertThat(RedesSociales.instagram("@autos.sur")).contains("autos.sur");
        assertThat(RedesSociales.instagram("https://www.instagram.com/autos_sur/?hl=es")).contains("autos_sur");
        assertThat(RedesSociales.instagram("  ")).isEmpty();
    }

    @Test
    void extraeElUsuarioDeFacebook() {
        assertThat(RedesSociales.facebook("https://facebook.com/AutosDelSur")).contains("AutosDelSur");
        assertThat(RedesSociales.facebook("autos.del-sur")).contains("autos.del-sur");
    }

    @Test
    void rechazaUsuariosInvalidos() {
        assertThatThrownBy(() -> RedesSociales.instagram("autos del sur"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
