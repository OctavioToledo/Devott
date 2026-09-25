package com.devott.ubicaciones;

import com.devott.compartido.errores.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Consulta la API Georef del Gobierno argentino (https://apis.datos.gob.ar/georef).
 * Las respuestas se cachean: las localidades casi no cambian.
 */
@Component
class GeorefCliente implements ProveedorDeLocalidades {

    /** Se piden más de las que se devuelven porque Georef repite localidades con el mismo nombre. */
    static final int MAXIMO_PEDIDO = 20;

    private final RestClient http;

    GeorefCliente(RestClient.Builder builder, @Value("${devott.georef.url}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    @Override
    @Cacheable("localidades")
    public List<Localidad> buscar(String texto) {
        RespuestaGeoref respuesta;
        try {
            respuesta = http.get()
                    .uri(uri -> uri.path("/localidades")
                            .queryParam("nombre", texto)
                            .queryParam("max", MAXIMO_PEDIDO)
                            .queryParam("campos", "id,nombre,provincia.nombre,centroide")
                            .build())
                    .retrieve()
                    .body(RespuestaGeoref.class);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No pudimos buscar localidades. Probá de nuevo en un rato.", e);
        }
        if (respuesta == null || respuesta.localidades() == null) {
            return List.of();
        }
        return respuesta.localidades().stream()
                .filter(l -> l.centroide() != null && l.provincia() != null)
                .map(l -> new Localidad(l.id(), l.nombre(), l.provincia().nombre(),
                        l.centroide().lat(), l.centroide().lon()))
                .toList();
    }

    record RespuestaGeoref(List<LocalidadGeoref> localidades) {
    }

    record LocalidadGeoref(String id, String nombre, Nombre provincia, Centroide centroide) {
    }

    record Nombre(String nombre) {
    }

    record Centroide(double lat, double lon) {
    }
}
