"use server";

import { revalidatePath } from "next/cache";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import { tokenDeAcceso } from "@/lib/auth/sesion";

export type ResultadoInteraccion = { ok: true } | { ok: false; necesitaIngreso: boolean; mensaje: string };

async function alternar(ruta: string, activar: boolean): Promise<ResultadoInteraccion> {
  if (!(await tokenDeAcceso())) {
    return { ok: false, necesitaIngreso: true, mensaje: "Ingresá para continuar." };
  }
  try {
    await pedirApiServidor(ruta, { metodo: activar ? "PUT" : "DELETE" });
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    return { ok: false, necesitaIngreso: e.status === 401, mensaje: e.detalle };
  }
  revalidatePath("/guardados");
  return { ok: true };
}

/** Guarda o quita una publicación de guardados (el corazón). */
export async function alternarGuardado(slug: string, guardar: boolean): Promise<ResultadoInteraccion> {
  return alternar(`/me/guardados/${encodeURIComponent(slug)}`, guardar);
}

/** Sigue o deja de seguir a un vendedor. */
export async function alternarSeguimiento(vendedorSlug: string, seguir: boolean): Promise<ResultadoInteraccion> {
  return alternar(`/me/seguimientos/${encodeURIComponent(vendedorSlug)}`, seguir);
}
