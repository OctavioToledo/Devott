import { NextResponse, type NextRequest } from "next/server";
import { destinoSeguro } from "@/lib/auth/redireccion";
import { supabaseServidor } from "@/lib/supabase/servidor";

/** Vuelta del proveedor (Google, etc.): cambia el código por la sesión y sigue a donde iba el usuario. */
export async function GET(request: NextRequest) {
  const { searchParams, origin } = request.nextUrl;
  const codigo = searchParams.get("code");
  const siguiente = destinoSeguro(searchParams.get("siguiente"));

  const supabase = await supabaseServidor();
  if (codigo && supabase) {
    const { error } = await supabase.auth.exchangeCodeForSession(codigo);
    if (!error) {
      return NextResponse.redirect(new URL(siguiente, origin));
    }
  }

  const ingreso = new URL("/ingresar", origin);
  ingreso.searchParams.set("error", "1");
  ingreso.searchParams.set("siguiente", siguiente);
  return NextResponse.redirect(ingreso);
}
