package com.devott.publicaciones;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite usar los códigos de la API ("0KM", "4X4") también en query params y path variables.
 */
@Configuration
class PublicacionesWebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Condicion.class, Condicion::deCodigo);
        registry.addConverter(String.class, Traccion.class, Traccion::deCodigo);
    }
}
