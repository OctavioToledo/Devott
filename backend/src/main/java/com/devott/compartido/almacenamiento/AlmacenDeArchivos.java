package com.devott.compartido.almacenamiento;

import java.util.Collection;
import java.util.Set;

public interface AlmacenDeArchivos {

    /** Tipos de imagen que se aceptan. El navegador las convierte a WebP (o JPEG si no puede). */
    Set<String> TIPOS_PERMITIDOS = Set.of("image/webp", "image/jpeg");

    static String extension(String contentType) {
        return "image/jpeg".equals(contentType) ? "jpg" : "webp";
    }

    /** URL firmada para subir un archivo a `ruta`. Vence en pocos minutos. */
    SubidaFirmada firmarSubida(String ruta, String contentType);

    /** URL pública del archivo. */
    String urlPublica(String ruta);

    /** Si el archivo ya se subió. Se usa al confirmar una subida. */
    boolean existe(String ruta);

    /** Borra los archivos. No falla si alguno no existe. */
    void eliminar(Collection<String> rutas);
}
