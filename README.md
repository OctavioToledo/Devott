# Devott — Contexto del proyecto

Marketplace de vehículos para Argentina. Concesionarias chicas y particulares pagan una suscripción económica para publicar su stock y obtienen un perfil público con link propio (devott.com/slug) para compartir en Instagram y Facebook. El comprador navega sin fricción, filtra por zona y características, y contacta al vendedor por WhatsApp con un botón.

Objetivo de producto: que buscar auto en Devott sea más cómodo que en Facebook Marketplace o Mercado Libre, gracias a buenos filtros, búsqueda por zona y una interfaz ágil pensada para mobile.

## Alcance del MVP

Incluye:
- Feed de publicaciones con filtros (zona y radio, 0 km / usado, concesionaria / particular, marca, modelo, precio ARS/USD, año, km, carrocería, combustible, transmisión, financiación, permuta, único dueño).
- Detalle de vehículo con botón de contacto por WhatsApp.
- Perfil público del vendedor con su stock.
- Cuentas con login social (Google y otros vía Supabase Auth).
- Comprador: guardar publicaciones (corazón) y seguir vendedores.
- Vendedor: perfil editable, CRUD de publicaciones con fotos, marcar vendido, panel con métricas básicas (visitas al perfil, vistas de publicaciones, clics a WhatsApp, guardados).
- Planes con límite de publicaciones y fotos.

Fuera del MVP (no implementar todavía):
- Reseñas de vendedores (el modelo de datos ya las contempla; se activan en una segunda etapa).
- Publicación automática en Facebook, Instagram o Mercado Libre.
- Notificaciones a seguidores.
- Contabilidad o gestión interna de concesionarias.

## Arquitectura

Monolito modular. Nada de microservicios.

- `frontend/`: Next.js (App Router, TypeScript). Deploy en Vercel. Las páginas públicas (feed, detalle, perfil) se renderizan en el servidor para SEO y tienen metadatos Open Graph para que los links se vean bien al compartirlos en WhatsApp e Instagram.
- `backend/`: Java 21 + Spring Boot (última versión estable). API REST JSON bajo `/api/v1`, documentada con OpenAPI (springdoc). Dockerizado; deploy en AWS, región sa-east-1 (São Paulo).
- Base de datos: Postgres de Supabase con la extensión PostGIS. Misma región que el backend.

### Reglas sobre Supabase (importante)

- El frontend usa Supabase SOLO para: login (Supabase Auth) y subir fotos mediante URLs firmadas que entrega el backend.
- Todo lo demás pasa por la API de Spring. El frontend nunca lee ni escribe tablas directamente.
- La API automática de Supabase (PostgREST) queda desactivada, o todas las tablas tienen RLS activado sin políticas.
- Spring valida el JWT de Supabase como OAuth2 resource server. El `sub` del token es el `usuario.id`.
- Solo Spring se conecta a Postgres (vía el pooler de Supabase). El esquema se versiona con Flyway, nunca a mano.
- Bucket de fotos: lectura pública, escritura solo con URL firmada.

### Flujo de subida de fotos

1. El frontend comprime y redimensiona la imagen en el navegador (máx. ~1600 px de ancho, WebP).
2. Pide a la API una URL de subida: `POST /api/v1/me/publicaciones/{id}/fotos/url-subida`. La API valida que la publicación sea del usuario y el límite de fotos del plan.
3. El frontend sube el archivo directo a Supabase Storage con esa URL.
4. El frontend confirma: `POST /api/v1/me/publicaciones/{id}/fotos`. La API registra la foto.

### Contacto por WhatsApp

El botón apunta a `GET /api/v1/contacto/{publicacionId}`. La API registra el evento en `contacto` (con `usuario_id` si hay sesión) y responde 302 a `https://wa.me/<numero>?text=<mensaje precargado con link a la publicación>`. El número del vendedor no se expone en el HTML.

## Estructura del repo

```
devott/
  CLAUDE.md
  README.md
  backend/     Spring Boot
  frontend/    Next.js
  infra/       docker-compose para desarrollo local, Dockerfiles
  docs/        decisiones de arquitectura
```

Paquetes del backend (`com.devott`), organizados por dominio:
- `compartido`: configuración, seguridad, manejo de errores, utilidades.
- `usuarios`: usuario (sincronizado con Supabase Auth).
- `vendedores`: perfil de vendedor.
- `catalogo`: marcas y modelos.
- `publicaciones`: publicaciones, fotos, búsqueda y filtros.
- `interacciones`: guardados, seguimientos, contactos, reseñas.
- `metricas`: métricas diarias agregadas.
- `suscripciones`: planes y suscripciones.

Desarrollo local: Postgres + PostGIS en Docker (imagen `postgis/postgis`) para no depender de la base remota. Auth sigue siendo Supabase también en local.

## Modelo de datos

- `usuario`: id (uuid, mismo que Supabase Auth), email, nombre, avatar_url, creado_en.
- `vendedor`: id, usuario_id (FK, único), tipo (`CONCESIONARIA` | `PARTICULAR`), slug (único), nombre_publico, whatsapp, telefono, descripcion, logo_path, direccion, ciudad, provincia, ubicacion (geography Point), horarios, instagram, facebook, verificado, creado_en.
- `plan`: id, nombre, max_publicaciones, max_fotos, precio_ars, activo.
- `suscripcion`: id, vendedor_id, plan_id, estado, inicio, vence_el, proveedor_pago_ref.
- `marca`: id, nombre, slug.
- `modelo`: id, marca_id, nombre, slug.
- `publicacion`: id, vendedor_id, modelo_id, version, anio, km, condicion (`0KM` | `USADO`), precio, moneda (`ARS` | `USD`), precio_usd_ref, carroceria, combustible, transmision, traccion, color, puertas, financia, acepta_permuta, unico_dueno, descripcion, ubicacion (geography Point), ciudad, provincia, estado (`BORRADOR` | `ACTIVA` | `PAUSADA` | `VENDIDA`), slug, publicada_en, vendida_en, actualizada_en.
- `foto`: id, publicacion_id, storage_path, orden, ancho, alto.
- `guardado`: PK (usuario_id, publicacion_id), creado_en.
- `seguimiento`: PK (usuario_id, vendedor_id), creado_en.
- `resena` (etapa 2): id, vendedor_id, usuario_id, puntaje 1–5, comentario, respuesta_vendedor, estado; único (vendedor_id, usuario_id). Solo puede reseñar quien tenga un registro en `contacto` con ese vendedor.
- `contacto`: id, publicacion_id, vendedor_id, usuario_id (nullable), creado_en.
- `metrica_diaria`: PK (fecha, vendedor_id, publicacion_id nullable, tipo), cantidad. Tipos: `VISTA_PERFIL`, `VISTA_PUBLICACION`.
- `cotizacion`: fecha, tipo, valor. Se usa para recalcular `precio_usd_ref`.

Precios: se guarda el precio original y su moneda. Filtros y orden por precio usan `precio_usd_ref`. La interfaz muestra el precio original.

Índices clave: GIST sobre `publicacion.ubicacion`; índices sobre (estado, condicion), precio_usd_ref, modelo_id, anio, km. Búsqueda por radio con `ST_DWithin`.

## Endpoints (borrador)

Públicos:
- `GET /api/v1/publicaciones` con filtros y paginación
- `GET /api/v1/publicaciones/{slug}`
- `GET /api/v1/vendedores/{slug}`
- `GET /api/v1/vendedores/{slug}/publicaciones`
- `GET /api/v1/catalogo/marcas`, `GET /api/v1/catalogo/marcas/{id}/modelos`
- `GET /api/v1/contacto/{publicacionId}` (registra y redirige a WhatsApp)
- `POST /api/v1/eventos/vista` (vista de perfil o publicación, con deduplicación)

Autenticados:
- `GET /api/v1/me`
- `GET|POST|DELETE /api/v1/me/guardados`
- `GET|POST|DELETE /api/v1/me/seguimientos`
- `GET|POST|PUT /api/v1/me/vendedor`
- `GET|POST|PUT|DELETE /api/v1/me/publicaciones`, `PATCH .../{id}/estado`
- `POST /api/v1/me/publicaciones/{id}/fotos/url-subida`, `POST|DELETE .../fotos`
- `GET /api/v1/me/metricas`

## Diseño

Los mockups aprobados están en el canvas de diseño (pantallas: Explorar, Filtros, Detalle, Perfil público, Panel del vendedor). Mobile first.

Tokens:
- Fondo `#F3EEE5`, superficie `#FFFFFF`, tinta `#17150F`, texto secundario `#5E574B`, bordes `#E2DACB` / `#D6CCBA`.
- Naranja marca y acciones de publicar: `#C2410C`.
- Verde exclusivo para WhatsApp: `#0E7A3F`.
- Azul confianza (verificado, información, seguridad): `#1E4F8A`, celeste `#DCEBF7`, azul profundo `#173A66`.
- Tipografías: Bricolage Grotesque (títulos, precios), Figtree (texto).
- Kilometraje mostrado como odómetro (dígitos en cajas oscuras).
- Áreas táctiles de al menos 44 px.

Textos de la interfaz en español rioplatense, con voseo ("Buscá", "Publicá tu auto").

## Convenciones

- Secretos en variables de entorno (`.env` fuera del repo; incluir `.env.example`).
- Commits pequeños y descriptivos.
- Tests: unitarios para lógica de dominio; tests de integración del backend con Testcontainers (Postgres + PostGIS).
- Antes de implementar algo grande, proponer un plan y esperar aprobación.

## Hoja de ruta

- [ ] 0. Estructura del monorepo, README, docker-compose local, CI básico
- [ ] 1. Backend base: health check, Flyway V1 con el esquema, validación JWT de Supabase, manejo de errores, OpenAPI
- [ ] 2. Frontend base: Next.js, tokens de diseño, layout, login con Supabase
- [ ] 3. Catálogo de marcas y modelos (seed)
- [ ] 4. Perfil de vendedor (alta y edición) + perfil público
- [ ] 5. Publicaciones: CRUD, fotos, estados
- [ ] 6. Feed y búsqueda con filtros y radio
- [ ] 7. Detalle + contacto por WhatsApp
- [ ] 8. Guardados y seguimientos
- [ ] 9. Métricas y panel del vendedor
- [ ] 10. Planes y límites (cobro manual al inicio)
- [ ] 11. Deploy: Vercel + AWS + dominio
- [ ] 12. Piloto con concesionarias reales
