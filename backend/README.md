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

## Base de datos

El esquema se versiona con Flyway en `src/main/resources/db/migration`. Nunca se modifica a mano ni se edita una migración ya aplicada: cada cambio es una migración nueva (`V2__...sql`). Hibernate solo valida que las entidades coincidan con el esquema.

Contra Supabase, la conexión va por el pooler. En modo transacción (puerto 6543), agregá `prepareThreshold=0` a la URL JDBC. Antes de la primera migración, activá la extensión PostGIS desde el panel de Supabase.

## Autenticación

La API valida los access tokens de Supabase Auth: firma (con las claves públicas del JWKS del proyecto), emisor y audiencia `authenticated`. El proyecto de Supabase tiene que usar claves de firma asimétricas (Settings > JWT Keys). El secreto HS256 heredado no está soportado.

- Públicos: los `GET` de publicaciones, vendedores, catálogo y contacto, `POST /api/v1/eventos/vista`, health y documentación.
- Todo lo demás exige `Authorization: Bearer <token>`.
- `GET /api/v1/me` crea o actualiza el usuario con los datos del token.

## Errores

Todas las respuestas de error usan `application/problem+json` (RFC 9457), con `title` y `detail` en español. Los errores de validación agregan `errores: [{campo, mensaje}]`. Desde el código se lanzan `RecursoNoEncontradoException` (404) y `ConflictoException` (409), de `compartido.errores`.

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

Health check: http://localhost:8080/actuator/health (más `/liveness` y `/readiness` para el deploy).
