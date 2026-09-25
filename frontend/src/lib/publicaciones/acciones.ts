"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { EstadoPublicacion, MiPublicacion, PublicacionRequest, SubidaFirmada } from "@/lib/api/tipos";

export type ResultadoGuardadoPublicacion = {
  mensaje: string;
  errores: Record<string, string>;
};

/** Refresca el panel y las páginas públicas donde aparece la publicación. */
async function refrescar(publicacion?: Pick<MiPublicacion, "slug">) {
  revalidatePath("/panel");
  if (publicacion) revalidatePath(`/publicaciones/${publicacion.slug}`);
  // El stock del perfil público depende de las publicaciones activas.
  revalidatePath("/[slug]", "page");
}

/**
 * Crea (en borrador) o edita una publicación. Al crearla sigue a su página de edición, donde se
 * cargan las fotos; al editarla vuelve al panel.
 */
export async function guardarPublicacion(
  id: string | null,
  datos: PublicacionRequest,
): Promise<ResultadoGuardadoPublicacion> {
  let guardada: MiPublicacion;
  try {
    guardada = await pedirApiServidor<MiPublicacion>(
      id ? `/me/publicaciones/${encodeURIComponent(id)}` : "/me/publicaciones",
      { metodo: id ? "PUT" : "POST", cuerpo: datos },
    );
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    const errores: Record<string, string> = {};
    for (const { campo, mensaje } of e.errores) {
      const clave = campo.startsWith("localidad") ? "localidad" : campo;
      errores[clave] ??= mensaje;
    }
    return { mensaje: e.detalle, errores };
  }

  await refrescar(guardada);
  redirect(id ? `/panel?publicacion=guardada` : `/panel/publicaciones/${guardada.id}?nueva=1`);
}

/** Cambia el estado. Devuelve el mensaje de error de la API, o null si salió bien. */
export async function cambiarEstadoPublicacion(id: string, estado: EstadoPublicacion): Promise<string | null> {
  try {
    const publicacion = await pedirApiServidor<MiPublicacion>(`/me/publicaciones/${encodeURIComponent(id)}/estado`, {
      metodo: "PATCH",
      cuerpo: { estado },
    });
    await refrescar(publicacion);
    return null;
  } catch (e) {
    if (e instanceof ErrorApi) return e.detalle;
    throw e;
  }
}

export async function eliminarPublicacion(id: string): Promise<string | null> {
  try {
    await pedirApiServidor(`/me/publicaciones/${encodeURIComponent(id)}`, { metodo: "DELETE" });
    await refrescar();
    return null;
  } catch (e) {
    if (e instanceof ErrorApi) return e.detalle;
    throw e;
  }
}

export type Resultado<T> = { ok: true; valor: T } | { ok: false; mensaje: string };

async function intentar<T>(pedido: () => Promise<T>): Promise<Resultado<T>> {
  try {
    return { ok: true, valor: await pedido() };
  } catch (e) {
    if (e instanceof ErrorApi) return { ok: false, mensaje: e.errores[0]?.mensaje ?? e.detalle };
    throw e;
  }
}

const rutaFotos = (id: string) => `/me/publicaciones/${encodeURIComponent(id)}/fotos`;

export async function pedirSubidaFoto(publicacionId: string, contentType: string) {
  return intentar(() =>
    pedirApiServidor<SubidaFirmada>(`${rutaFotos(publicacionId)}/url-subida`, { metodo: "POST", cuerpo: { contentType } }),
  );
}

export async function confirmarFoto(publicacionId: string, ruta: string, ancho: number, alto: number) {
  const resultado = await intentar(() =>
    pedirApiServidor<MiPublicacion>(rutaFotos(publicacionId), { metodo: "POST", cuerpo: { ruta, ancho, alto } }),
  );
  if (resultado.ok) await refrescar(resultado.valor);
  return resultado;
}

export async function eliminarFoto(publicacionId: string, fotoId: string) {
  const resultado = await intentar(() =>
    pedirApiServidor<MiPublicacion>(`${rutaFotos(publicacionId)}/${encodeURIComponent(fotoId)}`, { metodo: "DELETE" }),
  );
  if (resultado.ok) await refrescar(resultado.valor);
  return resultado;
}

export async function reordenarFotos(publicacionId: string, fotoIds: string[]) {
  const resultado = await intentar(() =>
    pedirApiServidor<MiPublicacion>(`${rutaFotos(publicacionId)}/orden`, { metodo: "PUT", cuerpo: { fotoIds } }),
  );
  if (resultado.ok) await refrescar(resultado.valor);
  return resultado;
}
