package com.devott.compartido.almacenamiento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Datos para que el navegador suba un archivo directo al almacenamiento: hace un `metodo` a `url`
 * con los `headers` indicados y el archivo como cuerpo, antes de `venceEn`.
 */
@Schema(name = "SubidaFirmada")
public record SubidaFirmada(String ruta, String url, String metodo, Map<String, String> headers, Instant venceEn) {
}
