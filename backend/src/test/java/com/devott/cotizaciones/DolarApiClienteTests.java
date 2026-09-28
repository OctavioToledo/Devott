package com.devott.cotizaciones;

import com.devott.compartido.errores.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DolarApiClienteTests {

    MockRestServiceServer servidor;
    DolarApiCliente cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new DolarApiCliente(builder, "https://dolarapi.test");
    }

    @Test
    void tomaElValorDeVentaConLaFechaArgentina() {
        // 01:30 UTC del 29 todavía es el 28 en Argentina.
        servidor.expect(requestTo("https://dolarapi.test/v1/dolares/blue"))
                .andRespond(withSuccess("""
                        {"moneda": "USD", "casa": "blue", "nombre": "Blue", "compra": 1545, "venta": 1565.5,
                         "fechaActualizacion": "2026-09-29T01:30:00.000Z"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cliente.actual(TipoCotizacion.BLUE))
                .isEqualTo(new Cotizacion(TipoCotizacion.BLUE, LocalDate.of(2026, 9, 28), new BigDecimal("1565.5")));
    }

    @Test
    void usaElNombreDeCasaDeDolarApi() {
        servidor.expect(requestTo("https://dolarapi.test/v1/dolares/bolsa"))
                .andRespond(withSuccess("""
                        {"venta": 1500, "fechaActualizacion": "2026-09-28T15:00:00.000Z"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cliente.actual(TipoCotizacion.MEP).valor()).isEqualByComparingTo("1500");
    }

    @Test
    void siDolarApiFallaAvisaQueNoEstaDisponible() {
        servidor.expect(requestTo("https://dolarapi.test/v1/dolares/blue"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> cliente.actual(TipoCotizacion.BLUE))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void rechazaUnaCotizacionSinValor() {
        servidor.expect(requestTo("https://dolarapi.test/v1/dolares/blue"))
                .andRespond(withSuccess("""
                        {"venta": null, "fechaActualizacion": "2026-09-28T15:00:00.000Z"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.actual(TipoCotizacion.BLUE))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
