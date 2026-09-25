import "server-only";
import { redirect } from "next/navigation";
import { cache } from "react";
import { supabaseServidor } from "@/lib/supabase/servidor";
import { urlDeIngreso } from "./redireccion";

export type UsuarioSesion = {
  id: string;
  email?: string;
  nombre?: string;
  avatarUrl?: string;
};

/** Usuario logueado según el token de Supabase (validado), o null. Se calcula una vez por request. */
export const usuarioActual = cache(async (): Promise<UsuarioSesion | null> => {
  const supabase = await supabaseServidor();
  if (!supabase) return null;

  const { data } = await supabase.auth.getClaims();
  const claims = data?.claims;
  if (!claims) return null;

  const metadata = (claims.user_metadata ?? {}) as Record<string, unknown>;
  const texto = (valor: unknown) => (typeof valor === "string" && valor ? valor : undefined);
  return {
    id: claims.sub,
    email: texto(claims.email),
    nombre: texto(metadata.full_name) ?? texto(metadata.name),
    avatarUrl: texto(metadata.avatar_url) ?? texto(metadata.picture),
  };
});

/** Para páginas privadas: devuelve el usuario o manda a /ingresar y vuelve a `ruta` después. */
export async function exigirUsuario(ruta: string): Promise<UsuarioSesion> {
  const usuario = await usuarioActual();
  if (!usuario) redirect(urlDeIngreso(ruta));
  return usuario;
}

/** Access token para llamar a la API de Spring en nombre del usuario, o null si no hay sesión. */
export async function tokenDeAcceso(): Promise<string | null> {
  const supabase = await supabaseServidor();
  if (!supabase) return null;
  const { data } = await supabase.auth.getSession();
  return data.session?.access_token ?? null;
}
