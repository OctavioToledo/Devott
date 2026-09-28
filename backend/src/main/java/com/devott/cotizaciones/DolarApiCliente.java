package com.devott.cotizaciones;

import com.devott.compartido.errores.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Consulta las cotizaciones de https://dolarapi.com.
 */
@Component
class DolarApiCliente implements ProveedorDeCotizaciones {

    static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final RestClient http;

    DolarApiCliente(RestClient.Builder builder, @Value("${devott.cotizaciones.url}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    @Override
    public Cotizacion actual(TipoCotizacion tipo) {
        RespuestaDolarApi respuesta;
        try {
            respuesta = http.get()
                    .uri("/v1/dolares/{casa}", tipo.casa)
                    .retrieve()
                    .body(RespuestaDolarApi.class);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No pudimos obtener la cotización del dólar.", e);
        }
        if (respuesta == null || respuesta.venta() == null || respuesta.venta().signum() <= 0
                || respuesta.fechaActualizacion() == null) {
            throw new ServicioNoDisponibleException("dolarapi.com devolvió una cotización inválida: " + respuesta, null);
        }
        return new Cotizacion(tipo, respuesta.fechaActualizacion().atZone(ARGENTINA).toLocalDate(), respuesta.venta());
    }

    record RespuestaDolarApi(BigDecimal venta, Instant fechaActualizacion) {
    }
}
