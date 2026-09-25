import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";
import { configSupabase } from "./config";

/**
 * Refresca la sesión de Supabase en cada request y devuelve la respuesta con las cookies
 * actualizadas, junto con si hay un usuario logueado.
 */
export async function actualizarSesion(
  request: NextRequest,
): Promise<{ respuesta: NextResponse; logueado: boolean }> {
  let respuesta = NextResponse.next({ request });
  const config = configSupabase();
  if (!config) return { respuesta, logueado: false };

  const supabase = createServerClient(config.url, config.clave, {
    cookies: {
      getAll() {
        return request.cookies.getAll();
      },
      setAll(cookiesAGuardar) {
        cookiesAGuardar.forEach(({ name, value }) => request.cookies.set(name, value));
        respuesta = NextResponse.next({ request });
        cookiesAGuardar.forEach(({ name, value, options }) => respuesta.cookies.set(name, value, options));
      },
    },
  });

  // No quitar: getClaims valida el token y dispara el refresco de la sesión si está por vencer.
  const { data } = await supabase.auth.getClaims();
  return { respuesta, logueado: Boolean(data?.claims) };
}
