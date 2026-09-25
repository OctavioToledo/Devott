package com.devott.compartido.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_SUPABASE = "supabase";

    @Bean
    OpenAPI devottOpenApi(@Value("${devott.api.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Devott API")
                        .description("API REST del marketplace de vehículos Devott.")
                        .version(version))
                .components(new Components().addSecuritySchemes(ESQUEMA_SUPABASE, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token de Supabase Auth del usuario logueado.")));
    }

    /**
     * Marca como autenticados los endpoints bajo /api/v1/me, así Swagger UI les envía el token.
     */
    @Bean
    OpenApiCustomizer seguridadEnEndpointsPrivados() {
        return openApi -> openApi.getPaths().forEach((ruta, item) -> {
            if (ruta.equals("/api/v1/me") || ruta.startsWith("/api/v1/me/")) {
                item.readOperations().forEach(op ->
                        op.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SUPABASE)));
            }
        });
    }
}
