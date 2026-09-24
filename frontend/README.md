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
npm run build   # build de producción
npm start       # sirve el build
```

## Reglas

- Los datos se leen y escriben siempre a través de la API de Spring (`NEXT_PUBLIC_API_URL`).
- Supabase se usa solo para login (Supabase Auth) y para subir fotos con las URLs firmadas que entrega el backend. Nunca se consultan tablas directamente.
- Las páginas públicas (feed, detalle, perfil) se renderizan en el servidor y llevan metadatos Open Graph.
- Textos de la interfaz en español rioplatense, con voseo.
