import { createBrowserClient } from "@supabase/ssr";
import { configSupabase } from "./config";

/** Cliente de Supabase para componentes de cliente. Solo para Auth y subida de fotos. */
export function supabaseNavegador() {
  const config = configSupabase();
  if (!config) {
    throw new Error("Falta configurar Supabase (NEXT_PUBLIC_SUPABASE_URL y NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY).");
  }
  return createBrowserClient(config.url, config.clave);
}
