package com.devott.compartido.almacenamiento;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Sirve y recibe los archivos del almacenamiento local. Solo existe en desarrollo.
 */
@Hidden
@RestController
@ConditionalOnBean(AlmacenamientoLocal.class)
class AlmacenamientoLocalController {

    /** Igual que en el frontend: las fotos llegan comprimidas, 10 MB sobra. */
    static final long TAMANO_MAXIMO = 10L * 1024 * 1024;

    private final AlmacenamientoLocal almacen;

    AlmacenamientoLocalController(AlmacenamientoLocal almacen) {
        this.almacen = almacen;
    }

    @PutMapping(AlmacenamientoLocal.PREFIJO + "/subir/**")
    ResponseEntity<Void> subir(HttpServletRequest request,
                               @RequestParam long vence,
                               @RequestParam String firma,
                               @RequestHeader(value = "Content-Type", required = false) String contentType)
            throws IOException {
        String ruta = rutaDesde(request, AlmacenamientoLocal.PREFIJO + "/subir/");
        if (!AlmacenamientoLocal.esRutaValida(ruta) || !almacen.firmaValida(ruta, vence, firma)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!esImagenPermitida(contentType)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        if (request.getContentLengthLong() > TAMANO_MAXIMO) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }
        almacen.guardar(ruta, request.getInputStream());
        return ResponseEntity.ok().build();
    }

    @GetMapping(AlmacenamientoLocal.PREFIJO + "/**")
    ResponseEntity<Resource> servir(HttpServletRequest request) throws IOException {
        String ruta = rutaDesde(request, AlmacenamientoLocal.PREFIJO + "/");
        if (!AlmacenamientoLocal.esRutaValida(ruta) || !almacen.existe(ruta)) {
            return ResponseEntity.notFound().build();
        }
        var archivo = almacen.archivo(ruta);
        String tipo = Files.probeContentType(archivo);
        return ResponseEntity.ok()
                .contentType(tipo != null ? MediaType.parseMediaType(tipo) : MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(new FileSystemResource(archivo));
    }

    /** Compara solo tipo y subtipo: "image/webp;charset=UTF-8" también vale. */
    static boolean esImagenPermitida(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType tipo = MediaType.parseMediaType(contentType);
            return AlmacenDeArchivos.TIPOS_PERMITIDOS.contains(tipo.getType() + "/" + tipo.getSubtype());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String rutaDesde(HttpServletRequest request, String prefijo) {
        String uri = request.getRequestURI().substring(request.getContextPath().length());
        return uri.startsWith(prefijo) ? uri.substring(prefijo.length()) : "";
    }
}
