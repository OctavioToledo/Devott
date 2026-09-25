"use server";

import { redirect } from "next/navigation";
import { supabaseServidor } from "@/lib/supabase/servidor";

export async function cerrarSesion() {
  const supabase = await supabaseServidor();
  await supabase?.auth.signOut();
  redirect("/");
}
