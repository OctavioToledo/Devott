"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { MiVendedor, SlugDisponible, SubidaFirmada, VendedorRequest } from "@/lib/api/tipos";

export type ResultadoGuardado = {
  mensaje: string;
  /** Error por campo del formulario. Los de la localidad ("localidad.lat") se agrupan en "localidad". */
  errores: Record<string, string>;
};

/**
 * Crea o edita el perfil del vendedor logueado. Si sale bien, vuelve al panel; si no, devuelve
 * los errores para mostrarlos en el formulario.
 */
export async function guardarPerfil(
  modo: "alta" | "edicion",
  datos: VendedorRequest,
  slugAnterior?: string,
): Promise<ResultadoGuardado> {
  let guardado: MiVendedor;
  try {
    guardado = await pedirApiServidor<MiVendedor>("/me/vendedor", {
      metodo: modo === "alta" ? "POST" : "PUT",
      cuerpo: datos,
    });
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    const errores: Record<string, string> = {};
    for (const { campo, mensaje } of e.errores) {
      const clave = campo.startsWith("localidad") ? "localidad" : campo;
      errores[clave] ??= mensaje;
    }
    return { mensaje: e.detalle, errores };
  }

  revalidatePath(`/${guardado.slug}`);
  if (slugAnterior && slugAnterior !== guardado.slug) revalidatePath(`/${slugAnterior}`);
  redirect(`/panel?guardado=${modo}`);
}

/** Consulta si un link de perfil está libre. Devuelve null si no se pudo consultar. */
export async function consultarSlug(slug: string): Promise<SlugDisponible | null> {
  try {
    return await pedirApiServidor<SlugDisponible>(`/me/vendedor/slug-disponible?slug=${encodeURIComponent(slug)}`);
  } catch {
    return null;
  }
}

export type ResultadoLogo = { ok: true; vendedor: MiVendedor } | { ok: false; mensaje: string };

export async function pedirSubidaLogo(
  contentType: string,
): Promise<{ ok: true; subida: SubidaFirmada } | { ok: false; mensaje: string }> {
  try {
    const subida = await pedirApiServidor<SubidaFirmada>("/me/vendedor/logo/url-subida", {
      metodo: "POST",
      cuerpo: { contentType },
    });
    return { ok: true, subida };
  } catch (e) {
    if (e instanceof ErrorApi) return { ok: false, mensaje: e.errores[0]?.mensaje ?? e.detalle };
    throw e;
  }
}

/** Confirma el logo subido (ruta) o lo quita (null). */
export async function guardarLogo(ruta: string | null): Promise<ResultadoLogo> {
  try {
    const vendedor = await pedirApiServidor<MiVendedor>("/me/vendedor/logo", {
      metodo: ruta ? "PUT" : "DELETE",
      cuerpo: ruta ? { ruta } : undefined,
    });
    revalidatePath(`/${vendedor.slug}`);
    revalidatePath("/panel", "layout");
    return { ok: true, vendedor };
  } catch (e) {
    if (e instanceof ErrorApi) return { ok: false, mensaje: e.errores[0]?.mensaje ?? e.detalle };
    throw e;
  }
}
