# Devott — Infra

Entorno local de desarrollo. Por ahora solo la base de datos: Postgres 17 + PostGIS 3.5, igual que en Supabase.

## Base de datos

```bash
cp .env.example .env
docker compose up -d        # levanta Postgres + PostGIS
docker compose ps           # debe mostrar "healthy"
docker compose logs -f db   # ver logs
docker compose down         # detener (los datos se conservan)
docker compose down -v      # detener y borrar los datos
```

Conexión: `postgresql://devott:devott@localhost:5432/devott` (con los valores por defecto).

```bash
docker compose exec db psql -U devott -d devott -c "SELECT postgis_version();"
```

La extensión `postgis` se crea con los scripts de `init/`, que corren solo cuando el volumen está vacío. Las tablas no se crean acá: el esquema lo maneja Flyway desde el backend.

Los Dockerfiles para producción se agregan en el paso de deploy.
