-- Vistas ya contadas, para no contar dos veces la misma vista de un visitante en el día.
-- `visitante` es el SHA-256 del id anónimo que genera el navegador. Se purgan las de más de dos días.
CREATE TABLE vista_registrada (
    fecha      date NOT NULL,
    visitante  text NOT NULL,
    tipo       text NOT NULL CHECK (tipo IN ('VISTA_PERFIL', 'VISTA_PUBLICACION')),
    objeto_id  uuid NOT NULL,
    PRIMARY KEY (fecha, visitante, tipo, objeto_id)
);

ALTER TABLE vista_registrada ENABLE ROW LEVEL SECURITY;
