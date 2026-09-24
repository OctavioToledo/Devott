# Devott

Marketplace de vehículos para Argentina. Concesionarias chicas y particulares publican su stock con un perfil propio para compartir en redes; los compradores filtran por zona y características y contactan por WhatsApp.

## Estructura

| Carpeta     | Contenido                                                        |
|-------------|------------------------------------------------------------------|
| `backend/`  | API REST en Java 21 + Spring Boot (Maven), documentada con OpenAPI |
| `frontend/` | Next.js (App Router, TypeScript, Tailwind)                       |
| `infra/`    | docker-compose con Postgres + PostGIS para desarrollo local      |
| `docs/`     | Decisiones de arquitectura (ADR)                                 |

## Requisitos

- Java 21
- Node.js 20 o superior y npm
- Docker con Docker Compose

Maven no hace falta instalarlo: el backend trae el wrapper `./mvnw`.

## Levantar el entorno local

```bash
# 1. Base de datos
cd infra
cp .env.example .env
docker compose up -d

# 2. Backend (http://localhost:8080, Swagger UI en /swagger-ui.html)
cd ../backend
./mvnw spring-boot:run

# 3. Frontend (http://localhost:3000)
cd ../frontend
cp .env.example .env.local
npm install
npm run dev
```

Cada carpeta tiene su propio README con más detalle.
