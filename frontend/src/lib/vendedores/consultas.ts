import "server-only";
import { cache } from "react";
import { ErrorApi, pedirApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { MiVendedor, VendedorPublico } from "@/lib/api/tipos";

/** Perfil de vendedor del usuario logueado, o null si todavía no lo creó. */
export async function miVendedor(): Promise<MiVendedor | null> {
  try {
    return await pedirApiServidor<MiVendedor>("/me/vendedor");
  } catch (e) {
    if (e instanceof ErrorApi && e.status === 404) return null;
    throw e;
  }
}

/**
 * Perfil público de un vendedor, o null si no existe. Se reutiliza un minuto; al editar el perfil
 * se invalida con revalidatePath. `cache` evita pedirlo dos veces (metadatos y página).
 */
export const vendedorPublico = cache(async (slug: string): Promise<VendedorPublico | null> => {
  try {
    return await pedirApi<VendedorPublico>(`/vendedores/${encodeURIComponent(slug)}`, { revalidar: 60 });
  } catch (e) {
    if (e instanceof ErrorApi && e.status === 404) return null;
    throw e;
  }
});
