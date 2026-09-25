import { pedirApi } from "./cliente";
import type { Marca, Modelo } from "./tipos";

/** El catálogo casi no cambia: igual que la API, se reutiliza por una hora. */
const UNA_HORA = 3600;

/** Marcas ordenadas por nombre. */
export function obtenerMarcas(): Promise<Marca[]> {
  return pedirApi<Marca[]>("/catalogo/marcas", { revalidar: UNA_HORA });
}

/** Modelos de una marca, ordenados por nombre (los numéricos por su valor: 206 antes que 2008). */
export function obtenerModelos(marcaId: number): Promise<Modelo[]> {
  return pedirApi<Modelo[]>(`/catalogo/marcas/${marcaId}/modelos`, { revalidar: UNA_HORA });
}
