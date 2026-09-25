import "server-only";
import { tokenDeAcceso } from "@/lib/auth/sesion";
import { pedirApi, type OpcionesPedido } from "./cliente";

/** Llama a la API desde el servidor, con el token del usuario logueado si hay sesión. */
export async function pedirApiServidor<T>(ruta: string, opciones: Omit<OpcionesPedido, "token"> = {}) {
  return pedirApi<T>(ruta, { ...opciones, token: await tokenDeAcceso() });
}
