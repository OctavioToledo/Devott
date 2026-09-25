import { NextResponse, type NextRequest } from "next/server";
import { tokenDeAcceso } from "@/lib/auth/sesion";
import { urlDeLaApi } from "@/lib/config";

/**
 * Botón de WhatsApp del perfil. Pasa por acá (y no directo a la API) para mandar el token si hay
 * sesión: así el contacto queda asociado al comprador. La API registra el contacto y responde con
 * la redirección a wa.me; el número nunca aparece en el HTML.
 */
export async function GET(request: NextRequest, { params }: RouteContext<"/contacto/vendedor/[slug]">) {
  const { slug } = await params;
  const token = await tokenDeAcceso();

  try {
    const respuesta = await fetch(`${urlDeLaApi()}/contacto/vendedores/${encodeURIComponent(slug)}`, {
      redirect: "manual",
      cache: "no-store",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    const destino = respuesta.headers.get("Location");
    if (respuesta.status === 302 && destino?.startsWith("https://wa.me/")) {
      return NextResponse.redirect(destino, 302);
    }
  } catch {
    // Sin conexión con la API: se vuelve al perfil.
  }
  return NextResponse.redirect(new URL(`/${encodeURIComponent(slug)}?contacto=error`, request.url), 302);
}
