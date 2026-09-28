-- Planes con cobro manual. Sin suscripción vigente no se puede publicar (no hay plan gratuito);
-- las pruebas gratis son suscripciones marcadas con es_prueba.

ALTER TABLE plan ADD COLUMN codigo text;
ALTER TABLE plan ADD COLUMN orden integer NOT NULL DEFAULT 0;

INSERT INTO plan (codigo, nombre, max_publicaciones, max_fotos, precio_ars, orden) VALUES
    ('PARTICULAR', 'Particular', 5, 8, 15000, 1),
    ('CONCESIONARIA', 'Concesionaria', 20, 8, 35000, 2),
    ('CONCESIONARIA_PLUS', 'Concesionaria Plus', 100, 10, 55000, 3);

ALTER TABLE plan ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE plan ADD CONSTRAINT plan_codigo_key UNIQUE (codigo);
ALTER TABLE plan ADD CONSTRAINT plan_codigo_check CHECK (codigo ~ '^[A-Z]+(_[A-Z]+)*$');

ALTER TABLE suscripcion ADD COLUMN es_prueba boolean NOT NULL DEFAULT false;
ALTER TABLE suscripcion ADD COLUMN creada_en timestamptz NOT NULL DEFAULT now();
-- Con cobro manual siempre hay fecha de fin.
ALTER TABLE suscripcion ALTER COLUMN vence_el SET NOT NULL;

-- A lo sumo una suscripción activa por vendedor.
CREATE UNIQUE INDEX suscripcion_una_activa_idx ON suscripcion (vendedor_id) WHERE estado = 'ACTIVA';
CREATE INDEX suscripcion_estado_vence_idx ON suscripcion (estado, vence_el);
