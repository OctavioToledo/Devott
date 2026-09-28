import type { NextRequest } from "next/server";
import { redirigirAWhatsapp } from "@/lib/contacto/whatsapp";

/** Botón de WhatsApp del perfil del vendedor. */
export async function GET(request: NextRequest, { params }: RouteContext<"/contacto/vendedor/[slug]">) {
  const { slug } = await params;
  const seguro = encodeURIComponent(slug);
  return redirigirAWhatsapp(`/contacto/vendedores/${seguro}`, new URL(`/${seguro}`, request.url));
}
