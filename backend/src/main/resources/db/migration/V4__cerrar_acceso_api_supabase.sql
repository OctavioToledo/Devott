-- En Supabase, los roles de la API automática (anon, authenticated) reciben permisos sobre toda tabla nueva de
-- public. Solo Spring accede a la base, así que se los sacamos: sobre lo que ya existe (incluida
-- flyway_schema_history, que no tiene RLS) y sobre lo que creen las migraciones siguientes.
-- Es una capa más, además de RLS sin políticas y de la Data API desactivada.
-- Fuera de Supabase (local, tests) esos roles no existen y no hace nada.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon')
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;
        REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM anon, authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM anon, authenticated;
        REVOKE USAGE ON SCHEMA public FROM anon, authenticated;
    END IF;
END
$$;
