package com.devott.compartido.almacenamiento;

import com.devott.compartido.errores.ServicioNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Supabase Storage. El bucket es de lectura pública y la escritura solo se hace con URLs firmadas
 * que genera este backend con la clave secreta del proyecto.
 */
@Component
@ConditionalOnProperty(name = "devott.almacenamiento.tipo", havingValue = "supabase")
class AlmacenamientoSupabase implements AlmacenDeArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoSupabase.class);

    /** Supabase firma las subidas por dos horas; se informa un vencimiento más corto por las dudas. */
    private static final Duration VIGENCIA = Duration.ofMinutes(30);

    private final RestClient http;
    private final String urlStorage;
    private final String bucket;

    AlmacenamientoSupabase(RestClient.Builder builder,
                           @Value("${devott.supabase.url}") String supabaseUrl,
                           @Value("${devott.supabase.secret-key}") String claveSecreta,
                           @Value("${devott.almacenamiento.bucket}") String bucket) {
        if (claveSecreta == null || claveSecreta.isBlank()) {
            throw new IllegalStateException("Falta SUPABASE_SECRET_KEY para usar Supabase Storage.");
        }
        this.urlStorage = supabaseUrl + "/storage/v1";
        this.bucket = bucket;
        this.http = builder
                .baseUrl(urlStorage)
                .defaultHeaders(h -> {
                    h.set("apikey", claveSecreta);
                    // Las claves heredadas (service_role) son JWT y van también como Bearer.
                    if (claveSecreta.startsWith("eyJ")) {
                        h.setBearerAuth(claveSecreta);
                    }
                })
                .build();
    }

    @Override
    public SubidaFirmada firmarSubida(String ruta, String contentType) {
        RespuestaFirma firma;
        try {
            firma = http.post()
                    .uri("/object/upload/sign/{bucket}/{ruta}", bucket, ruta)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(RespuestaFirma.class);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No pudimos preparar la subida. Probá de nuevo en un rato.", e);
        }
        if (firma == null || firma.url() == null) {
            throw new ServicioNoDisponibleException("No pudimos preparar la subida. Probá de nuevo en un rato.", null);
        }
        return new SubidaFirmada(ruta, urlStorage + firma.url(), "PUT",
                Map.of(HttpHeaders.CONTENT_TYPE, contentType, "x-upsert", "false"),
                Instant.now().plus(VIGENCIA));
    }

    @Override
    public String urlPublica(String ruta) {
        return urlStorage + "/object/public/" + bucket + "/" + ruta;
    }

    @Override
    public boolean existe(String ruta) {
        try {
            HttpStatusCode estado = http.head()
                    .uri("/object/public/{bucket}/{ruta}", bucket, ruta)
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { })
                    .toBodilessEntity()
                    .getStatusCode();
            return estado.is2xxSuccessful();
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No pudimos verificar la foto. Probá de nuevo en un rato.", e);
        }
    }

    @Override
    public void eliminar(Collection<String> rutas) {
        if (rutas.isEmpty()) {
            return;
        }
        try {
            http.method(HttpMethod.DELETE)
                    .uri("/object/{bucket}", bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.copyOf(rutas)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // No se corta la operación del usuario: quedan archivos huérfanos que se pueden limpiar después.
            log.warn("No se pudieron borrar {} archivos de Supabase Storage", rutas.size(), e);
        }
    }

    record RespuestaFirma(String url) {
    }
}
