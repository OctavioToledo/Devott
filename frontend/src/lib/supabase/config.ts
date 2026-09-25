export type ConfigSupabase = { url: string; clave: string };

/**
 * Datos públicos del proyecto de Supabase. Devuelve null si faltan: la app funciona igual,
 * sin login, hasta que se configure el proyecto.
 */
export function configSupabase(): ConfigSupabase | null {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const clave = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;
  return url && clave ? { url, clave } : null;
}
