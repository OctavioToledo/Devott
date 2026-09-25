package com.devott.vendedores;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappTests {

    @Test
    void aceptaElNumeroConCodigoDeArea() {
        assertThat(Whatsapp.normalizar("3534 123456")).contains("5493534123456");
        assertThat(Whatsapp.normalizar("11-4567-8901")).contains("5491145678901");
    }

    @Test
    void aceptaElCeroAdelanteYLosPrefijosInternacionales() {
        assertThat(Whatsapp.normalizar("0353 4123456")).contains("5493534123456");
        assertThat(Whatsapp.normalizar("+54 9 353 412-3456")).contains("5493534123456");
        assertThat(Whatsapp.normalizar("54 353 4123456")).contains("5493534123456");
    }

    @Test
    void rechazaNumerosIncompletosOConQuince() {
        assertThat(Whatsapp.normalizar("4123456")).isEmpty();
        assertThat(Whatsapp.normalizar("0353 15 4123456")).isEmpty();
        assertThat(Whatsapp.normalizar("")).isEmpty();
        assertThat(Whatsapp.normalizar(null)).isEmpty();
    }

    @Test
    void muestraElNumeroLocal() {
        assertThat(Whatsapp.local("5493534123456")).isEqualTo("3534123456");
    }
}
