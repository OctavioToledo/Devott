# Devott — Frontend

Next.js (App Router) + TypeScript + Tailwind CSS. Se despliega en Vercel.

## Correr

```bash
cp .env.example .env.local   # completar valores
npm install
npm run dev                  # http://localhost:3000
```

Otros scripts:

```bash
npm run lint    # ESLint
npm test        # tests unitarios (Vitest)
npm run build   # build de producción
npm start       # sirve el build
```

## Reglas

- Los datos se leen y escriben siempre a través de la API de Spring (`NEXT_PUBLIC_API_URL`).
- Supabase se usa solo para login (Supabase Auth) y para subir fotos con las URLs firmadas que entrega el backend. Nunca se consultan tablas directamente.
- Las páginas públicas (feed, detalle, perfil) se renderizan en el servidor y llevan metadatos Open Graph.
- Textos de la interfaz en español rioplatense, con voseo.

## Estructura

| Carpeta                 | Contenido                                                        |
|-------------------------|------------------------------------------------------------------|
| `src/app/(sitio)`       | Páginas con el encabezado del sitio: inicio, `/ingresar`, `/cuenta`, `/panel` |
| `src/app/[slug]`        | Perfil público del vendedor (`devott.com/slug`), con su propio banner |
| `src/app/contacto`      | Redirecciones a WhatsApp: pasan el token a la API y siguen a `wa.me` |
| `src/components/ui`     | Componentes base (`Boton`, `BotonLink`, `Logo`)                  |
| `src/components/layout` | Encabezado y demás piezas del layout                             |
| `src/lib/api`           | Cliente de la API de Spring y tipos de sus respuestas            |
| `src/lib/auth`          | Sesión, rutas privadas y server actions de login                 |
| `src/lib/vendedores`    | Consultas, server actions y formato del perfil de vendedor       |
| `src/lib/supabase`      | Clientes de Supabase (navegador, servidor y proxy)               |
| `src/proxy.ts`          | Refresca la sesión en cada request y protege las rutas privadas  |

## Rutas de primer nivel y slugs

Los perfiles viven en `devott.com/<slug>`, así que cada ruta de primer nivel del sitio (`/panel`, `/ingresar`, `/contacto`…) tiene que estar en la lista de slugs reservados del backend (`SlugsReservados.java`). Si agregás una ruta nueva en `src/app`, sumala ahí.

## Diseño

Los tokens están en `src/app/globals.css` como tema de Tailwind y se usan como clases: `bg-fondo`, `bg-superficie`, `text-tinta`, `text-secundario`, `border-borde`, `bg-marca`, `bg-whatsapp`, `text-confianza`, `bg-celeste`, `font-titulo` (Bricolage Grotesque). El texto usa Figtree por defecto. Los botones tienen al menos 44 px de alto.

## Login con Supabase

Sin las variables de Supabase la app funciona igual, pero sin login. Para activarlo:

1. Creá el proyecto en Supabase, en la región São Paulo (`sa-east-1`).
2. En **Settings > API Keys**, copiá la URL y la *publishable key* (`sb_publishable_...`) a `.env.local`.
3. En **Settings > JWT Keys**, pasá a claves de firma asimétricas. El backend valida los tokens con ellas.
4. En **Authentication > Sign In / Providers**, activá Google con el client ID y secret de Google Cloud. En Google Cloud, la URI de redirección autorizada es `https://<tu-proyecto>.supabase.co/auth/v1/callback`.
5. En **Authentication > URL Configuration**, poné `http://localhost:3000` como Site URL y agregá `http://localhost:3000/auth/callback` (y la URL de producción) a las Redirect URLs.
6. En el backend, completá `SUPABASE_URL` en `backend/.env`.

Para probar el circuito completo, levantá el backend y entrá a `/cuenta`: la página llama a `GET /api/v1/me` con tu token.

El login usa PKCE: el botón manda a Google, que vuelve a `/auth/callback`. Ahí se cambia el código por la sesión y se sigue a la página de la que venías (`?siguiente=`, solo rutas internas).
