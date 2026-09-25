# 0002 — Convenciones del esquema de base de datos

- Estado: Aceptado
- Fecha: 2026-09-25

## Contexto

El esquema inicial (Flyway `V1__esquema_inicial.sql`) implementa el modelo de datos del MVP. Algunas decisiones no surgen directamente del modelo y conviene dejarlas registradas.

## Decisión

- **Estados y tipos como `text` con `CHECK`**, no como enums de Postgres. Agregar un valor es cambiar un `CHECK` en una migración, y JPA los mapea como strings sin tipos custom.
- **Ids**: `uuid` con `gen_random_uuid()` para las entidades. `marca` y `modelo` usan `integer` identity porque son catálogo chico y estable. `usuario.id` no tiene default: es el id de Supabase Auth.
- **Sin FK a `auth.users`**: esa tabla no existe en la base local. El usuario se crea en `usuario` la primera vez que llama a `GET /api/v1/me`.
- **`metrica_diaria` con id propio**: `publicacion_id` es null en las vistas de perfil, y una columna de la PK no puede ser null. La unicidad se garantiza con `UNIQUE NULLS NOT DISTINCT (fecha, vendedor_id, publicacion_id, tipo)`, que requiere Postgres 15 o más.
- **`contacto` sobrevive al borrado de la publicación** (`publicacion_id` pasa a null): sigue contando para las métricas del vendedor y para habilitar reseñas.
- **`publicacion.creada_en`**: se agrega además de `actualizada_en`, que el modelo ya tenía.
- **RLS activado sin políticas en todas las tablas**, incluida la de historial de Flyway. Esa última se protege desde `FlywayConfig` después de migrar, porque dentro de una migración Flyway la tiene bloqueada.

## Consecuencias

- Los valores válidos de cada estado o tipo están en la migración, y los enums de Java tienen que coincidir con ellos. Hibernate valida el esquema al arrancar (`ddl-auto: validate`).
- `condicion` usa el valor `0KM`, que no es un nombre válido para una constante de enum en Java: hace falta un converter al mapear la entidad.
