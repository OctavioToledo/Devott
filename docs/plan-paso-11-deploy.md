# Paso 11: deploy (plan)

Estado: propuesto el 2026-09-28, pendiente de aprobación. Se implementa en la próxima sesión.

## Objetivo

Devott en producción: frontend en Vercel (`devott.com`), API en AWS São Paulo (`api.devott.com`),
base, login y fotos en Supabase (sa-east-1). Deploy automático al pushear a `main` si pasan los tests.

```
Navegador ──> devott.com (Vercel, Next.js) ──> api.devott.com (AWS sa-east-1, Spring Boot en Docker)
    │                                                   │
    └── login y subida de fotos ──> Supabase (sa-east-1): Auth, Storage, Postgres + PostGIS <──┘
```

## Lo que tenés que hacer vos (cuentas y datos)

1. **Dominio**: decidir `devott.com` o `devott.com.ar` (NIC Argentina) y comprarlo. Lo ideal es manejar el DNS en
   Cloudflare o Route 53.
2. **Supabase**: proyecto de producción en sa-east-1. Recomiendo el plan **Pro (USD 25/mes)**: el gratuito se
   **pausa tras una semana sin actividad** y no tiene backups diarios.
3. **Google Cloud Console**: credenciales OAuth para "Ingresar con Google", con las URLs de Supabase.
4. **AWS**: cuenta con facturación y una alerta de presupuesto.
5. **Vercel**: cuenta conectada al repo de GitHub.
6. **WhatsApp de ventas** para el botón de `/planes`.

## Qué implemento yo

### 1. Supabase de producción (con vos, paso a paso)
- Extensión PostGIS activada. Flyway crea el esquema al primer arranque (V1 a V3 y el catálogo).
- Conexión del backend por el **pooler en modo sesión** (puerto 5432). El modo transacción (6543) rompe los
  prepared statements de Hibernate y Flyway.
- API automática (Data API/PostgREST) desactivada; igual todas las tablas ya tienen RLS sin políticas.
- Auth: Google como proveedor, claves de firma asimétricas, URLs de redirección `https://devott.com/auth/callback`.
- Storage: bucket público `fotos`.

### 2. Backend listo para producción
- `backend/Dockerfile` multi-stage: build con Maven y runtime con Java 21 JRE, usuario no root y memoria de la JVM
  ajustada al contenedor.
- Configuración de producción por variables de entorno: `ALMACENAMIENTO=supabase`, `FRONTEND_URL` (CORS),
  `API_URL`, `DEVOTT_ADMINS`, datos de Supabase y de la base. Respetar `X-Forwarded-*` detrás del proxy.
- Swagger UI: decidir si queda público en producción (propuesta: sí, el API es público igual; se puede cerrar luego).
- Los secretos van a AWS (Secrets Manager o Parameter Store), nunca al repo.

### 3. AWS en São Paulo
Opción recomendada, simple y barata para el piloto:
- **Una instancia EC2 chica (t4g.small, ARM)** con Docker, el contenedor de la API y **Caddy** delante para HTTPS
  automático en `api.devott.com`. Unos USD 15/mes.
- Imagen en **ECR**; logs a CloudWatch; alerta de presupuesto.

Alternativa más administrada (más cara, unos USD 40/mes o más): **ECS Fargate + Application Load Balancer + ACM**.
A confirmar mañana: si **App Runner** está disponible en sa-east-1 sería la opción más simple; hay que verificarlo.

### 4. Deploy automático (GitHub Actions)
- Nuevo job que, en `main` y después de los tests, construye la imagen, la sube a ECR y la despliega.
- Autenticación a AWS por **OIDC** (rol de IAM sin claves guardadas en GitHub).
- El frontend lo despliega Vercel solo en cada push (y previews en los pull requests).

### 5. Vercel
- Proyecto con raíz `frontend/`, variables `NEXT_PUBLIC_SITE_URL`, `NEXT_PUBLIC_API_URL`,
  `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`, `NEXT_PUBLIC_WHATSAPP_VENTAS`.
- Región de funciones en São Paulo (`gru1`) para estar cerca de la API.
- Dominio `devott.com` (y `www` redirigiendo).

### 6. Dominio y DNS
- `devott.com` y `www` → Vercel. `api.devott.com` → AWS.

### 7. Verificación después del deploy
Lo que quedó "sin probar hasta conectar Supabase", ahora en producción:
- Login con Google de punta a punta; `/cuenta`.
- Crear perfil de vendedor, subir logo y fotos (Supabase Storage), publicar.
- Asignarte un plan desde `/admin` y ver "Tu plan" y las métricas en `/panel`.
- Corazón, Seguir y `/guardados` con sesión.
- Botón de WhatsApp y vistas registradas.
- Links compartidos en WhatsApp con su vista previa (Open Graph).
- Cotización del dólar y feed con filtros.

### 8. Antes del piloto (paso 12)
- Backups: los diarios de Supabase Pro; probar una restauración.
- Monitoreo mínimo: health check externo (por ejemplo UptimeRobot) sobre `/actuator/health`.
- Páginas legales básicas (términos y privacidad), porque se guardan datos de usuarios.

## Costos aproximados por mes (piloto)
| Servicio | Estimado |
|---|---|
| Supabase Pro | USD 25 |
| AWS (EC2 t4g.small, disco, ECR, logs) | USD 15–20 |
| Vercel Hobby (o Pro USD 20 si se usa comercialmente) | USD 0–20 |
| Dominio | USD 1–2 (prorrateado) |

## Commits previstos
1. Dockerfile y configuración de producción del backend.
2. Infraestructura (script o documentación paso a paso de AWS) y job de deploy en GitHub Actions.
3. Ajustes del frontend para producción (región, dominios) y documentación del deploy en los README.
