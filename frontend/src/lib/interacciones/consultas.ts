import "server-only";
import { cache } from "react";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { Pagina, TarjetaPublicacion, VendedorSeguido } from "@/lib/api/tipos";
import { usuarioActual } from "@/lib/auth/sesion";

/** Slugs de la API del usuario logueado; sin sesión o si la API falla, vacío (los botones igual funcionan). */
async function slugsDe(ruta: string): Promise<string[]> {
  if (!(await usuarioActual())) return [];
  try {
    return await pedirApiServidor<string[]>(ruta);
  } catch (e) {
    if (e instanceof ErrorApi) return [];
    throw e;
  }
}

/** Slugs de las publicaciones guardadas, para marcar los corazones. Una vez por request. */
export const slugsGuardados = cache(() => slugsDe("/me/guardados/slugs"));

/** Slugs de los vendedores seguidos, para el botón "Seguir". Una vez por request. */
export const slugsSeguidos = cache(() => slugsDe("/me/seguimientos/slugs"));

export function misGuardados(pagina = 0): Promise<Pagina<TarjetaPublicacion>> {
  return pedirApiServidor<Pagina<TarjetaPublicacion>>(`/me/guardados?pagina=${pagina}&tamano=60`);
}

export function misSeguimientos(): Promise<VendedorSeguido[]> {
  return pedirApiServidor<VendedorSeguido[]>("/me/seguimientos");
}
