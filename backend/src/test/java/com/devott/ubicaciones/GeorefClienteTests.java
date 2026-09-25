package com.devott.ubicaciones;

import com.devott.compartido.errores.ServicioNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class GeorefClienteTests {

    MockRestServiceServer servidor;
    GeorefCliente cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new GeorefCliente(builder, "https://georef.test/api");
    }

    @Test
    void convierteLaRespuestaDeGeoref() {
        servidor.expect(requestTo(startsWith("https://georef.test/api/localidades")))
                .andExpect(queryParam("nombre", "villa%20maria"))
                .andRespond(withSuccess("""
                        {"localidades": [
                          {"id": "14042170", "nombre": "Villa María", "provincia": {"nombre": "Córdoba"},
                           "centroide": {"lat": -32.41, "lon": -63.24}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscar("villa maria"))
                .containsExactly(new Localidad("14042170", "Villa María", "Córdoba", -32.41, -63.24));
    }

    @Test
    void siGeorefFallaAvisaQueNoEstaDisponible() {
        servidor.expect(requestTo(startsWith("https://georef.test/api/localidades")))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> cliente.buscar("rosario"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessage("No pudimos buscar localidades. Probá de nuevo en un rato.");
    }
}
