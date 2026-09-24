# 0001 — Monolito modular con Spring como único acceso a la base

- Estado: Aceptado
- Fecha: 2026-09-24

## Contexto

Devott es un MVP con un equipo chico. Necesitamos iterar rápido, mantener bajos los costos de infraestructura y tener SEO en las páginas públicas. Supabase ofrece Postgres, Auth y Storage administrados, pero también expone una API automática (PostgREST) sobre las tablas.

## Decisión

- **Monolito modular**: un solo backend en Java 21 + Spring Boot, organizado en paquetes por dominio (`usuarios`, `vendedores`, `catalogo`, `publicaciones`, `interacciones`, `metricas`, `suscripciones`, `compartido`). Nada de microservicios.
- **Frontend en Next.js** (App Router), con renderizado en servidor para las páginas públicas. Deploy en Vercel.
- **Backend en AWS sa-east-1**, en la misma región que la base de Supabase.
- **Spring es el único que accede a Postgres**, vía el pooler de Supabase. El esquema se versiona con Flyway.
- **El frontend usa Supabase solo para Auth y para subir fotos** con URLs firmadas que entrega el backend. La API automática de Supabase queda desactivada o todas las tablas tienen RLS sin políticas.
- **Spring valida el JWT de Supabase** como OAuth2 resource server; el `sub` del token es el `usuario.id`.
- **La API se documenta con OpenAPI** (springdoc) desde el primer endpoint.

## Consecuencias

- Toda la lógica de negocio y las reglas de acceso viven en un solo lugar (Spring) y se testean ahí.
- Un solo artefacto para desplegar y monitorear.
- Si en el futuro un módulo necesita escalar por separado, los límites por paquete facilitan extraerlo.
- Dependemos de Supabase para Auth y Storage; migrar implicaría reemplazar ambos, pero no la lógica de negocio.
