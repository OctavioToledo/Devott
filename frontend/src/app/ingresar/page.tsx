import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { destinoSeguro } from "@/lib/auth/redireccion";
import { usuarioActual } from "@/lib/auth/sesion";
import { configSupabase } from "@/lib/supabase/config";
import { BotonGoogle } from "./BotonGoogle";

export const metadata: Metadata = {
  title: "Ingresar",
  robots: { index: false },
};

export default async function Ingresar({ searchParams }: PageProps<"/ingresar">) {
  const params = await searchParams;
  const siguiente = destinoSeguro(typeof params.siguiente === "string" ? params.siguiente : null);
  const fallo = params.error !== undefined;

  if (await usuarioActual()) redirect(siguiente);

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-4 pt-8 pb-16">
      <div className="flex flex-col gap-2">
        <h1 className="font-titulo text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px]">
          Ingresá a Devott
        </h1>
        <p className="text-secundario">
          Guardá los autos que te gustan, seguí a tus concesionarias y publicá tu stock.
        </p>
      </div>

      <div className="flex flex-col gap-4 rounded-[22px] border border-borde bg-superficie p-5">
        {fallo && (
          <p role="alert" className="rounded-xl bg-celeste p-3 text-sm text-confianza-profundo">
            No pudimos completar el ingreso. Probá de nuevo.
          </p>
        )}
        {configSupabase() ? (
          <BotonGoogle siguiente={siguiente} />
        ) : (
          <p className="text-sm text-secundario">
            El ingreso todavía no está configurado. Completá las variables de Supabase en{" "}
            <code>.env.local</code>.
          </p>
        )}
        <p className="text-xs text-secundario">
          Para buscar autos no hace falta cuenta. Solo la necesitás para guardar, seguir o publicar.
        </p>
      </div>
    </main>
  );
}
