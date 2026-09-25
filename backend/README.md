# Devott — Backend

API REST de Devott. Java 21 + Spring Boot 4, Maven.

## Correr

```bash
./mvnw spring-boot:run   # levanta en http://localhost:8080
./mvnw test              # corre los tests
./mvnw verify            # compila, testea y empaqueta
```

No hace falta tener Maven instalado: `./mvnw` lo descarga la primera vez.

## Variables de entorno

Ver `.env.example`. Copialo a `.env` en esta carpeta: al correr desde `backend/`, Spring lo carga solo (`spring.config.import`). Las variables del entorno tienen prioridad sobre el archivo.

Para desarrollo local, levantá la base con `docker compose up -d` desde `infra/`.

## Tests

Los tests de integración levantan Postgres + PostGIS con Testcontainers (imagen `postgis/postgis:17-3.5`), así que necesitan Docker corriendo. No usan la base de docker-compose.

## Documentación de la API

Todos los endpoints bajo `/api/**` se documentan con OpenAPI (springdoc):

- Swagger UI: http://localhost:8080/swagger-ui.html
- Especificación JSON: http://localhost:8080/v3/api-docs

Cada controlador y DTO nuevo debe llevar sus anotaciones (`@Tag`, `@Operation`, `@Schema`, respuestas de error) para que la especificación quede completa.

## Estructura

Paquetes en `com.devott`, organizados por dominio:

| Paquete         | Contenido                                            |
|-----------------|------------------------------------------------------|
| `compartido`    | Configuración, seguridad, manejo de errores, utilidades |
| `usuarios`      | Usuarios (sincronizados con Supabase Auth)           |
| `vendedores`    | Perfil de vendedor                                   |
| `catalogo`      | Marcas y modelos                                     |
| `publicaciones` | Publicaciones, fotos, búsqueda y filtros             |
| `interacciones` | Guardados, seguimientos, contactos, reseñas          |
| `metricas`      | Métricas diarias agregadas                           |
| `suscripciones` | Planes y suscripciones                               |

Health check: http://localhost:8080/actuator/health
