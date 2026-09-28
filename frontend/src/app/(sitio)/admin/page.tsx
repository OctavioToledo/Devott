import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { GestionPlan } from "@/components/admin/GestionPlan";
import { clasesBoton } from "@/components/ui/boton";
import { claseEntrada } from "@/components/ui/Campo";
import { pedirApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { Me, Plan, VendedorAdmin } from "@/lib/api/tipos";
import { exigirUsuario } from "@/lib/auth/sesion";
import { fechaDelPlan } from "@/lib/planes/formato";

export const metadata: Metadata = {
  title: "Administración",
  robots: { index: false },
};

/** Hoy en Argentina, "2026-09-28". */
function hoyEnArgentina(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Argentina/Buenos_Aires" }).format(new Date());
}

export default async function Admin({ searchParams }: PageProps<"/admin">) {
  await exigirUsuario("/admin");
  const me = await pedirApiServidor<Me>("/me").catch(() => null);
  // A quien no administra no se le confirma que la página existe.
  if (!me?.admin) notFound();

  const { q } = await searchParams;
  const texto = (Array.isArray(q) ? q[0] : q)?.trim() ?? "";
  const [planes, porVencer, vendedores] = await Promise.all([
    pedirApi<Plan[]>("/planes"),
    pedirApiServidor<VendedorAdmin[]>("/admin/suscripciones/por-vencer?dias=14"),
    pedirApiServidor<VendedorAdmin[]>(`/admin/vendedores?q=${encodeURIComponent(texto)}`),
  ]);
  const hoy = hoyEnArgentina();

  return (
    <main className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-8 px-4 pt-6 pb-16">
      <h1 className="font-titulo text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px]">Administración</h1>

      <section aria-labelledby="por-vencer" className="flex flex-col gap-3">
        <h2 id="por-vencer" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
          Por vencer (14 días) y en gracia
        </h2>
        {porVencer.length > 0 ? (
          <ListaVendedores vendedores={porVencer} planes={planes} hoy={hoy} />
        ) : (
          <p className="text-sm text-secundario">No hay planes por vencer.</p>
        )}
      </section>

      <section aria-labelledby="vendedores" className="flex flex-col gap-3">
        <h2 id="vendedores" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
          Vendedores
        </h2>
        <form className="flex gap-2" role="search">
          <label htmlFor="buscar-vendedor" className="sr-only">
            Buscar por nombre, link o email
          </label>
          <input id="buscar-vendedor" name="q" defaultValue={texto} placeholder="Nombre, link o email" className={claseEntrada} />
          <button type="submit" className={clasesBoton("oscuro", "lg", "shrink-0")}>
            Buscar
          </button>
        </form>
        <p className="text-sm text-secundario">
          {texto ? `Resultados para “${texto}”` : "Los últimos 30 vendedores que se registraron."}
        </p>
        {vendedores.length > 0 ? (
          <ListaVendedores vendedores={vendedores} planes={planes} hoy={hoy} />
        ) : (
          <p className="text-sm text-secundario">No encontramos vendedores.</p>
        )}
      </section>
    </main>
  );
}

function ListaVendedores({ vendedores, planes, hoy }: { vendedores: VendedorAdmin[]; planes: Plan[]; hoy: string }) {
  return (
    <ul className="flex flex-col gap-3">
      {vendedores.map((v) => {
        const s = v.suscripcion;
        const lugar = [v.ciudad, v.provincia].filter(Boolean).join(", ");
        return (
          <li key={v.slug} className="flex flex-col gap-3 rounded-[18px] border border-borde bg-superficie p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <Link href={`/${v.slug}`} className="font-bold">
                  {v.nombrePublico}
                </Link>
                <p className="text-sm text-secundario">
                  {v.tipo === "CONCESIONARIA" ? "Concesionaria" : "Particular"}
                  {lugar && ` · ${lugar}`}
                  {v.email && ` · ${v.email}`}
                </p>
                <p className="text-sm text-secundario">{v.publicacionesActivas} autos publicados</p>
              </div>
              <div className="text-right text-sm">
                {s ? (
                  <>
                    <p className="font-semibold">
                      {s.nombrePlan}
                      {s.esPrueba && " (prueba)"}
                    </p>
                    <p className={s.enGracia ? "font-semibold text-marca-oscuro" : "text-secundario"}>
                      {s.enGracia ? "Venció el " : "Vence el "}
                      {fechaDelPlan(s.venceEl)}
                    </p>
                    {s.referencia && <p className="text-secundario">{s.referencia}</p>}
                  </>
                ) : (
                  <p className="font-semibold text-secundario">Sin plan</p>
                )}
              </div>
            </div>
            <GestionPlan vendedor={v} planes={planes} hoy={hoy} />
          </li>
        );
      })}
    </ul>
  );
}
