package com.devott.interacciones;

import com.devott.publicaciones.Moneda;
import com.devott.publicaciones.PublicacionService.ParaContacto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MensajeWhatsappTests {

    @Test
    void incluyeAutoVersionAnioPrecioYLink() {
        var p = new ParaContacto(UUID.randomUUID(), UUID.randomUUID(), "toyota-hilux-2021-k3f9x2", "Toyota Hilux",
                "2.8 SRV 4x4 AT", 2021, new BigDecimal("34900.00"), Moneda.USD);

        assertThat(ContactoService.mensajePorPublicacion(p, "https://devott.com")).isEqualTo("""
                ¡Hola! Vi tu Toyota Hilux 2.8 SRV 4x4 AT 2021 publicado en Devott a US$ 34.900 y quería \
                consultarte si sigue disponible.
                https://devott.com/publicaciones/toyota-hilux-2021-k3f9x2""");
    }

    @Test
    void sinVersionYEnPesos() {
        var p = new ParaContacto(UUID.randomUUID(), UUID.randomUUID(), "chevrolet-onix-2025-abc123", "Chevrolet Onix",
                null, 2025, new BigDecimal("25900000.00"), Moneda.ARS);

        assertThat(ContactoService.mensajePorPublicacion(p, "https://devott.com"))
                .startsWith("¡Hola! Vi tu Chevrolet Onix 2025 publicado en Devott a $ 25.900.000 y");
    }
}
