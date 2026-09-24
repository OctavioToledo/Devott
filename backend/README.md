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

Ver `.env.example`. Spring lee variables del entorno, no el archivo `.env` directamente: exportalas en tu shell o cargalas desde tu IDE.

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
