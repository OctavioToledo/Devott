"use server";

import { revalidatePath } from "next/cache";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";

export type ResultadoAdmin = { ok: true } | { ok: false; mensaje: string; errores: Record<string, string> };

async function ejecutar(ruta: string, metodo: "PUT" | "DELETE", cuerpo?: unknown): Promise<ResultadoAdmin> {
  try {
    await pedirApiServidor(ruta, { metodo, cuerpo });
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    return { ok: false, mensaje: e.detalle, errores: Object.fromEntries(e.errores.map((x) => [x.campo, x.mensaje])) };
  }
  revalidatePath("/admin");
  return { ok: true };
}

export async function asignarPlan(
  vendedorSlug: string,
  datos: { plan: string; venceEl: string; esPrueba: boolean; referencia: string },
): Promise<ResultadoAdmin> {
  return ejecutar(`/admin/vendedores/${encodeURIComponent(vendedorSlug)}/suscripcion`, "PUT", datos);
}

export async function cancelarPlan(vendedorSlug: string): Promise<ResultadoAdmin> {
  return ejecutar(`/admin/vendedores/${encodeURIComponent(vendedorSlug)}/suscripcion`, "DELETE");
}
