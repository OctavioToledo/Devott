package com.devott.compartido.almacenamiento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Almacenamiento en un directorio local, para desarrollar sin Supabase. Imita el contrato de
 * Supabase Storage: URLs de subida firmadas (HMAC, con vencimiento) y lectura pública.
 * Los archivos se sirven desde {@link AlmacenamientoLocalController}.
 */
@Component
@ConditionalOnProperty(name = "devott.almacenamiento.tipo", havingValue = "local", matchIfMissing = true)
public class AlmacenamientoLocal implements AlmacenDeArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoLocal.class);

    static final String PREFIJO = "/almacenamiento-local";
    static final Duration VIGENCIA = Duration.ofMinutes(15);

    /** Rutas relativas sin "..", barras dobles ni caracteres raros. */
    private static final Pattern RUTA_VALIDA = Pattern.compile("^[a-z0-9-]+(/[a-zA-Z0-9._-]+)+$");

    private final Path directorio;
    private final String urlBase;
    private final byte[] secreto;
    private final Clock reloj;

    AlmacenamientoLocal(@Value("${devott.almacenamiento.local.directorio}") Path directorio,
                        @Value("${devott.api-url}") String urlApi,
                        Clock reloj) {
        this.directorio = directorio.toAbsolutePath().normalize();
        this.urlBase = urlApi + PREFIJO;
        this.reloj = reloj;
        // Un secreto nuevo en cada arranque alcanza: las URLs duran minutos.
        this.secreto = new byte[32];
        new SecureRandom().nextBytes(secreto);
        log.info("Almacenamiento local de archivos en {}", this.directorio);
    }

    @Override
    public SubidaFirmada firmarSubida(String ruta, String contentType) {
        validarRuta(ruta);
        Instant vence = Instant.now(reloj).plus(VIGENCIA);
        String url = UriComponentsBuilder.fromUriString(urlBase + "/subir/" + ruta)
                .queryParam("vence", vence.getEpochSecond())
                .queryParam("firma", firma(ruta, vence.getEpochSecond()))
                .build()
                .toUriString();
        return new SubidaFirmada(ruta, url, "PUT", Map.of(HttpHeaders.CONTENT_TYPE, contentType), vence);
    }

    @Override
    public String urlPublica(String ruta) {
        return urlBase + "/" + ruta;
    }

    @Override
    public boolean existe(String ruta) {
        return Files.isRegularFile(archivo(ruta));
    }

    @Override
    public void eliminar(Collection<String> rutas) {
        for (String ruta : rutas) {
            try {
                Files.deleteIfExists(archivo(ruta));
            } catch (IOException e) {
                log.warn("No se pudo borrar {}", ruta, e);
            }
        }
    }

    /** Verifica la firma de una subida. */
    boolean firmaValida(String ruta, long vence, String firmaRecibida) {
        if (Instant.now(reloj).getEpochSecond() > vence) {
            return false;
        }
        return MessageDigest.isEqual(firma(ruta, vence).getBytes(StandardCharsets.UTF_8),
                String.valueOf(firmaRecibida).getBytes(StandardCharsets.UTF_8));
    }

    void guardar(String ruta, InputStream contenido) {
        Path destino = archivo(ruta);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path archivo(String ruta) {
        validarRuta(ruta);
        Path archivo = directorio.resolve(ruta).normalize();
        if (!archivo.startsWith(directorio)) {
            throw new IllegalArgumentException("Ruta inválida: " + ruta);
        }
        return archivo;
    }

    static boolean esRutaValida(String ruta) {
        return ruta != null && RUTA_VALIDA.matcher(ruta).matches() && !ruta.contains("..");
    }

    private static void validarRuta(String ruta) {
        if (!esRutaValida(ruta)) {
            throw new IllegalArgumentException("Ruta inválida: " + ruta);
        }
    }

    private String firma(String ruta, long vence) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            byte[] resultado = mac.doFinal((ruta + "|" + vence).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
