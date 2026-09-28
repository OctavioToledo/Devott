import "server-only";
import { ErrorApi, pedirApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { Condicion, MiPublicacion, Pagina, TarjetaPublicacion } from "@/lib/api/tipos";
import { consultaApi, type FiltrosFeed } from "./busqueda";

export function misPublicaciones(): Promise<MiPublicacion[]> {
  return pedirApiServidor<MiPublicacion[]>("/me/publicaciones");
}

/** Una publicación propia, o null si no existe o es de otro vendedor. */
export async function miPublicacion(id: string): Promise<MiPublicacion | null> {
  try {
    return await pedirApiServidor<MiPublicacion>(`/me/publicaciones/${encodeURIComponent(id)}`);
  } catch (e) {
    if (e instanceof ErrorApi && (e.status === 404 || e.status === 400)) return null;
    throw e;
  }
}

/** Stock activo de un vendedor para su perfil público. Se reutiliza un minuto. */
export function stockDeVendedor(
  slug: string,
  condicion?: Condicion,
): Promise<Pagina<TarjetaPublicacion>> {
  const params = new URLSearchParams({ tamano: "60" });
  if (condicion) params.set("condicion", condicion);
  return pedirApi<Pagina<TarjetaPublicacion>>(`/vendedores/${encodeURIComponent(slug)}/publicaciones?${params}`, {
    revalidar: 60,
  });
}

/** Primera página del feed. Se reutiliza un minuto: es la misma para todos con los mismos filtros. */
export function buscarPublicaciones(filtros: FiltrosFeed): Promise<Pagina<TarjetaPublicacion>> {
  return pedirApi<Pagina<TarjetaPublicacion>>(`/publicaciones?${consultaApi(filtros)}`, { revalidar: 60 });
}
