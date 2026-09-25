-- Esquema inicial del MVP de Devott.
-- Estados y tipos se guardan como text con CHECK (no enums de Postgres) para simplificar JPA y futuras migraciones.

CREATE EXTENSION IF NOT EXISTS postgis;

-- Usuarios -------------------------------------------------------------------

-- El id es el mismo que en Supabase Auth (claim "sub" del JWT). No hay FK a auth.users
-- porque esa tabla no existe en la base local.
CREATE TABLE usuario (
    id          uuid PRIMARY KEY,
    email       text,
    nombre      text,
    avatar_url  text,
    creado_en   timestamptz NOT NULL DEFAULT now()
);

-- Vendedores -----------------------------------------------------------------

CREATE TABLE vendedor (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      uuid NOT NULL UNIQUE REFERENCES usuario (id) ON DELETE CASCADE,
    tipo            text NOT NULL CHECK (tipo IN ('CONCESIONARIA', 'PARTICULAR')),
    slug            text NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    nombre_publico  text NOT NULL,
    whatsapp        text NOT NULL,
    telefono        text,
    descripcion     text,
    logo_path       text,
    direccion       text,
    ciudad          text,
    provincia       text,
    ubicacion       geography(Point, 4326),
    horarios        text,
    instagram       text,
    facebook        text,
    verificado      boolean NOT NULL DEFAULT false,
    creado_en       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX vendedor_ubicacion_idx ON vendedor USING GIST (ubicacion);

-- Planes y suscripciones -----------------------------------------------------

CREATE TABLE plan (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre             text NOT NULL UNIQUE,
    max_publicaciones  integer NOT NULL CHECK (max_publicaciones > 0),
    max_fotos          integer NOT NULL CHECK (max_fotos > 0),
    precio_ars         numeric(12, 2) NOT NULL CHECK (precio_ars >= 0),
    activo             boolean NOT NULL DEFAULT true
);

CREATE TABLE suscripcion (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id         uuid NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    plan_id             uuid NOT NULL REFERENCES plan (id),
    estado              text NOT NULL CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'VENCIDA', 'CANCELADA')),
    inicio              date NOT NULL,
    vence_el            date,
    proveedor_pago_ref  text,
    CHECK (vence_el IS NULL OR vence_el >= inicio)
);

CREATE INDEX suscripcion_vendedor_idx ON suscripcion (vendedor_id);
CREATE INDEX suscripcion_plan_idx ON suscripcion (plan_id);
-- Un vendedor tiene como máximo una suscripción activa.
CREATE UNIQUE INDEX suscripcion_activa_unica_idx ON suscripcion (vendedor_id) WHERE estado = 'ACTIVA';

-- Catálogo -------------------------------------------------------------------

CREATE TABLE marca (
    id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre  text NOT NULL UNIQUE,
    slug    text NOT NULL UNIQUE
);

CREATE TABLE modelo (
    id        integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    marca_id  integer NOT NULL REFERENCES marca (id),
    nombre    text NOT NULL,
    slug      text NOT NULL,
    UNIQUE (marca_id, nombre),
    UNIQUE (marca_id, slug)
);

-- Publicaciones --------------------------------------------------------------

CREATE TABLE publicacion (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id     uuid NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    modelo_id       integer NOT NULL REFERENCES modelo (id),
    version         text,
    anio            integer NOT NULL CHECK (anio BETWEEN 1900 AND 2100),
    km              integer NOT NULL DEFAULT 0 CHECK (km >= 0),
    condicion       text NOT NULL CHECK (condicion IN ('0KM', 'USADO')),
    precio          numeric(14, 2) NOT NULL CHECK (precio > 0),
    moneda          text NOT NULL CHECK (moneda IN ('ARS', 'USD')),
    -- Precio convertido a USD con la última cotización. Filtros y orden usan esta columna.
    precio_usd_ref  numeric(14, 2),
    carroceria      text CHECK (carroceria IN ('SEDAN', 'HATCHBACK', 'SUV', 'PICKUP', 'COUPE', 'CONVERTIBLE',
                                               'RURAL', 'MONOVOLUMEN', 'UTILITARIO')),
    combustible     text CHECK (combustible IN ('NAFTA', 'DIESEL', 'GNC', 'HIBRIDO', 'ELECTRICO')),
    transmision     text CHECK (transmision IN ('MANUAL', 'AUTOMATICA')),
    traccion        text CHECK (traccion IN ('DELANTERA', 'TRASERA', '4X4', 'AWD')),
    color           text,
    puertas         integer CHECK (puertas BETWEEN 1 AND 6),
    financia        boolean NOT NULL DEFAULT false,
    acepta_permuta  boolean NOT NULL DEFAULT false,
    unico_dueno     boolean NOT NULL DEFAULT false,
    descripcion     text,
    ubicacion       geography(Point, 4326),
    ciudad          text,
    provincia       text,
    estado          text NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR', 'ACTIVA', 'PAUSADA', 'VENDIDA')),
    slug            text NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    publicada_en    timestamptz,
    vendida_en      timestamptz,
    creada_en       timestamptz NOT NULL DEFAULT now(),
    actualizada_en  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX publicacion_ubicacion_idx ON publicacion USING GIST (ubicacion);
CREATE INDEX publicacion_estado_condicion_idx ON publicacion (estado, condicion);
CREATE INDEX publicacion_precio_usd_ref_idx ON publicacion (precio_usd_ref);
CREATE INDEX publicacion_modelo_idx ON publicacion (modelo_id);
CREATE INDEX publicacion_anio_idx ON publicacion (anio);
CREATE INDEX publicacion_km_idx ON publicacion (km);
CREATE INDEX publicacion_vendedor_idx ON publicacion (vendedor_id);

CREATE TABLE foto (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  uuid NOT NULL REFERENCES publicacion (id) ON DELETE CASCADE,
    storage_path    text NOT NULL UNIQUE,
    orden           integer NOT NULL CHECK (orden >= 0),
    ancho           integer CHECK (ancho > 0),
    alto            integer CHECK (alto > 0),
    -- Diferida para poder reordenar las fotos dentro de una misma transacción.
    CONSTRAINT foto_publicacion_orden_key UNIQUE (publicacion_id, orden) DEFERRABLE INITIALLY DEFERRED
);

-- Interacciones --------------------------------------------------------------

CREATE TABLE guardado (
    usuario_id      uuid NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    publicacion_id  uuid NOT NULL REFERENCES publicacion (id) ON DELETE CASCADE,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, publicacion_id)
);

CREATE INDEX guardado_publicacion_idx ON guardado (publicacion_id);

CREATE TABLE seguimiento (
    usuario_id   uuid NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    vendedor_id  uuid NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    creado_en    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, vendedor_id)
);

CREATE INDEX seguimiento_vendedor_idx ON seguimiento (vendedor_id);

-- Si se borra la publicación, el contacto se conserva: sigue contando para las métricas
-- del vendedor y para habilitar reseñas.
CREATE TABLE contacto (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacion_id  uuid REFERENCES publicacion (id) ON DELETE SET NULL,
    vendedor_id     uuid NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    usuario_id      uuid REFERENCES usuario (id) ON DELETE SET NULL,
    creado_en       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX contacto_vendedor_creado_idx ON contacto (vendedor_id, creado_en);
CREATE INDEX contacto_publicacion_idx ON contacto (publicacion_id);
CREATE INDEX contacto_usuario_vendedor_idx ON contacto (usuario_id, vendedor_id);

-- Etapa 2: la tabla existe pero no se usa en el MVP.
-- Solo puede reseñar quien tenga un contacto con el vendedor (se valida en la aplicación).
CREATE TABLE resena (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id         uuid NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    usuario_id          uuid NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    puntaje             integer NOT NULL CHECK (puntaje BETWEEN 1 AND 5),
    comentario          text,
    respuesta_vendedor  text,
    estado              text NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'PUBLICADA', 'OCULTA')),
    creado_en           timestamptz NOT NULL DEFAULT now(),
    UNIQUE (vendedor_id, usuario_id)
);

CREATE INDEX resena_usuario_idx ON resena (usuario_id);

-- Métricas -------------------------------------------------------------------

-- publicacion_id es null para las vistas de perfil. Como una columna de la PK no puede ser null,
-- se usa un id propio y un índice único que trata los null como iguales.
CREATE TABLE metrica_diaria (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha           date NOT NULL,
    vendedor_id     uuid NOT NULL REFERENCES vendedor (id) ON DELETE CASCADE,
    publicacion_id  uuid REFERENCES publicacion (id) ON DELETE CASCADE,
    tipo            text NOT NULL CHECK (tipo IN ('VISTA_PERFIL', 'VISTA_PUBLICACION')),
    cantidad        integer NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    CONSTRAINT metrica_diaria_clave_key UNIQUE NULLS NOT DISTINCT (fecha, vendedor_id, publicacion_id, tipo),
    CHECK ((tipo = 'VISTA_PERFIL') = (publicacion_id IS NULL))
);

CREATE INDEX metrica_diaria_vendedor_fecha_idx ON metrica_diaria (vendedor_id, fecha);
CREATE INDEX metrica_diaria_publicacion_idx ON metrica_diaria (publicacion_id);

CREATE TABLE cotizacion (
    fecha  date NOT NULL,
    tipo   text NOT NULL CHECK (tipo IN ('OFICIAL', 'BLUE', 'MEP')),
    valor  numeric(12, 4) NOT NULL CHECK (valor > 0),
    PRIMARY KEY (fecha, tipo)
);

-- Seguridad en Supabase ------------------------------------------------------

-- RLS activado sin políticas: la API automática de Supabase (PostgREST) no puede leer ni escribir.
-- Spring se conecta como dueño de las tablas, por lo que RLS no le aplica.
-- La tabla de historial de Flyway se protege en FlywayConfig (acá quedaría bloqueada por Flyway).
ALTER TABLE usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendedor ENABLE ROW LEVEL SECURITY;
ALTER TABLE plan ENABLE ROW LEVEL SECURITY;
ALTER TABLE suscripcion ENABLE ROW LEVEL SECURITY;
ALTER TABLE marca ENABLE ROW LEVEL SECURITY;
ALTER TABLE modelo ENABLE ROW LEVEL SECURITY;
ALTER TABLE publicacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE foto ENABLE ROW LEVEL SECURITY;
ALTER TABLE guardado ENABLE ROW LEVEL SECURITY;
ALTER TABLE seguimiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacto ENABLE ROW LEVEL SECURITY;
ALTER TABLE resena ENABLE ROW LEVEL SECURITY;
ALTER TABLE metrica_diaria ENABLE ROW LEVEL SECURITY;
ALTER TABLE cotizacion ENABLE ROW LEVEL SECURITY;
