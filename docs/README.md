# Devott — Documentación

Decisiones de arquitectura registradas como ADR (Architecture Decision Records) en `adr/`.

Cada ADR es un archivo `NNNN-titulo-corto.md` con: contexto, decisión y consecuencias. Un ADR aceptado no se edita: si la decisión cambia, se escribe uno nuevo que lo reemplace.

| ADR | Título | Estado |
|-----|--------|--------|
| [0001](adr/0001-monolito-modular.md) | Monolito modular con Spring como único acceso a la base | Aceptado |
| [0002](adr/0002-esquema-de-base-de-datos.md) | Convenciones del esquema de base de datos | Aceptado |

La documentación de la API se genera con OpenAPI desde el backend (ver `backend/README.md`).
