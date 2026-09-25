import "server-only";
import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";
import { configSupabase } from "./config";

/**
 * Cliente de Supabase para componentes de servidor, server actions y route handlers.
 * Devuelve null si Supabase no está configurado.
 */
export async function supabaseServidor() {
  // Se leen las cookies antes de chequear la config para que la página siempre dependa de la sesión
  // y Next no la prerenderice como estática.
  const almacen = await cookies();
  const config = configSupabase();
  if (!config) return null;

  return createServerClient(config.url, config.clave, {
    cookies: {
      getAll() {
        return almacen.getAll();
      },
      setAll(cookiesAGuardar) {
        try {
          cookiesAGuardar.forEach(({ name, value, options }) => almacen.set(name, value, options));
        } catch {
          // Los componentes de servidor no pueden escribir cookies. No hace falta:
          // el proxy ya refrescó la sesión antes de renderizar.
        }
      },
    },
  });
}
