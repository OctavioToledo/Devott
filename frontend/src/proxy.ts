import { NextResponse, type NextRequest } from "next/server";
import { esRutaPrivada, urlDeIngreso } from "@/lib/auth/redireccion";
import { actualizarSesion } from "@/lib/supabase/proxy";

export async function proxy(request: NextRequest) {
  const { respuesta, logueado } = await actualizarSesion(request);
  const { pathname, search } = request.nextUrl;

  // Chequeo optimista: cada página privada vuelve a validar la sesión al renderizar.
  if (!logueado && esRutaPrivada(pathname)) {
    return NextResponse.redirect(new URL(urlDeIngreso(pathname + search), request.url));
  }
  return respuesta;
}

export const config = {
  // Todo menos archivos estáticos e imágenes.
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)"],
};
