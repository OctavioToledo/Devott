import "server-only";
import { NextResponse } from "next/server";
import { tokenDeAcceso } from "@/lib/auth/sesion";
import { urlDeLaApi } from "@/lib/config";

/**
 * Los botones de WhatsApp pasan por una ruta del sitio (y no directo a la API) para mandar el token si
 * hay sesión: así el contacto queda asociado al comprador. La API registra el contacto y responde con
 * la redirección a wa.me; el número nunca aparece en el HTML. Si algo falla, se vuelve a `siFalla`.
 */
export async function redirigirAWhatsapp(rutaApi: string, siFalla: URL): Promise<NextResponse> {
  const token = await tokenDeAcceso();
  try {
    const respuesta = await fetch(`${urlDeLaApi()}${rutaApi}`, {
      redirect: "manual",
      cache: "no-store",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    const destino = respuesta.headers.get("Location");
    if (respuesta.status === 302 && destino?.startsWith("https://wa.me/")) {
      return NextResponse.redirect(destino, 302);
    }
  } catch {
    // Sin conexión con la API: se vuelve a la página de origen.
  }
  siFalla.searchParams.set("contacto", "error");
  return NextResponse.redirect(siFalla, 302);
}
