import type { NextRequest } from "next/server";
import { redirigirAWhatsapp } from "@/lib/contacto/whatsapp";

/** Botón de WhatsApp de una publicación: el mensaje precargado dice qué auto es. */
export async function GET(request: NextRequest, { params }: RouteContext<"/contacto/publicacion/[slug]">) {
  const { slug } = await params;
  const seguro = encodeURIComponent(slug);
  return redirigirAWhatsapp(`/contacto/publicaciones/${seguro}`, new URL(`/publicaciones/${seguro}`, request.url));
}
