import Link from "next/link";
import type { MiPlan } from "@/lib/api/tipos";
import { fechaDelPlan } from "@/lib/planes/formato";

/** "Tu plan" en el panel: nombre, uso de publicaciones, vencimiento y avisos. */
export function TarjetaPlan({ plan: p }: { plan: MiPlan }) {
  if (!p.plan) {
    return (
      <section aria-labelledby="tu-plan" className="flex flex-col gap-3 rounded-[18px] border border-marca bg-superficie p-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 id="tu-plan" className="font-bold">Todavía no tenés un plan</h2>
          <p className="text-sm text-secundario">
            Podés cargar tus autos como borrador. Para publicarlos elegí un plan.
          </p>
        </div>
        <Link href="/planes" className="inline-flex min-h-11 shrink-0 items-center justify-center rounded-full bg-marca px-5 text-sm font-bold text-white no-underline hover:bg-marca-oscuro">
          Ver planes
        </Link>
      </section>
    );
  }

  const uso = Math.min(100, (p.publicacionesActivas / p.maxPublicaciones) * 100);
  const enGracia = p.avisos.includes("EN_GRACIA");
  const porVencer = p.avisos.includes("POR_VENCER");
  const enLimite = p.avisos.includes("LIMITE_ALCANZADO");

  return (
    <section aria-labelledby="tu-plan" className="flex flex-col gap-3 rounded-[18px] border border-borde bg-superficie p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="tu-plan" className="flex flex-wrap items-center gap-2 font-bold">
          Plan {p.plan.nombre}
          {p.esPrueba && (
            <span className="rounded-full bg-celeste px-2.5 py-0.5 text-xs font-bold text-confianza-profundo">Prueba gratis</span>
          )}
        </h2>
        {p.venceEl && (
          <p className="text-sm text-secundario">
            {p.esPrueba ? "Prueba hasta el " : "Pagado hasta el "}
            {fechaDelPlan(p.venceEl)}
          </p>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <p className="text-sm">
          <strong className="tabular-nums">{p.publicacionesActivas}</strong> de {p.maxPublicaciones} autos publicados ·{" "}
          {p.maxFotos} fotos por auto
        </p>
        {/* Medidor: el riel es un tono claro del mismo azul que el relleno. */}
        <div
          role="meter"
          aria-label="Autos publicados"
          aria-valuemin={0}
          aria-valuemax={p.maxPublicaciones}
          aria-valuenow={p.publicacionesActivas}
          className="h-2 overflow-hidden rounded-full bg-celeste"
        >
          <div className="h-full rounded-full" style={{ width: `${uso}%`, background: enLimite ? "var(--color-marca)" : "var(--color-grafico)" }} />
        </div>
      </div>

      {(enGracia || porVencer || enLimite) && (
        <ul className="flex flex-col gap-2">
          {enGracia && p.venceEl && p.pausaEl && (
            <Aviso fuerte>
              Tu plan venció el {fechaDelPlan(p.venceEl)}. Si no lo renovás, el {fechaDelPlan(p.pausaEl)} se pausan tus
              autos publicados.
            </Aviso>
          )}
          {porVencer && p.venceEl && (
            <Aviso>
              {p.esPrueba
                ? `Tu prueba termina el ${fechaDelPlan(p.venceEl)}. Elegí un plan para que tus autos sigan publicados.`
                : `Tu plan vence el ${fechaDelPlan(p.venceEl)}. Renovalo para que tus autos sigan publicados.`}
            </Aviso>
          )}
          {enLimite && <Aviso>Llegaste al máximo de autos de tu plan. Para publicar más, pasate a uno más grande.</Aviso>}
        </ul>
      )}

      <Link href="/planes" className="self-start text-sm font-semibold text-marca">
        {enGracia || porVencer ? "Renovar o cambiar de plan" : enLimite ? "Ver planes más grandes" : "Ver planes"}
      </Link>
    </section>
  );
}

function Aviso({ children, fuerte = false }: { children: React.ReactNode; fuerte?: boolean }) {
  return (
    <li
      role={fuerte ? "alert" : undefined}
      className={`flex items-start gap-2 rounded-2xl p-3 text-sm ${fuerte ? "bg-[#fbe4d6] text-marca-oscuro" : "bg-celeste text-confianza-profundo"}`}
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true" className="mt-px shrink-0">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7.5v5.5M12 16.5v.01" />
      </svg>
      <span>{children}</span>
    </li>
  );
}
