"use client";

import { useId, useState, useTransition } from "react";
import { clasesBoton } from "@/components/ui/boton";
import { claseEntrada } from "@/components/ui/Campo";
import { asignarPlan, cancelarPlan } from "@/lib/admin/acciones";
import type { Plan, VendedorAdmin } from "@/lib/api/tipos";
import { precioMensual } from "@/lib/planes/formato";

function sumarDias(iso: string, dias: number): string {
  const d = new Date(`${iso}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + dias);
  return d.toISOString().slice(0, 10);
}

/** Asignar, renovar o cancelar el plan de un vendedor. `hoy` viene del servidor (hora argentina). */
export function GestionPlan({ vendedor, planes, hoy }: { vendedor: VendedorAdmin; planes: Plan[]; hoy: string }) {
  const id = useId();
  const actual = vendedor.suscripcion;
  // Renovar: un mes más desde el vencimiento actual (si todavía no pasó), si no desde hoy.
  const base = actual && actual.venceEl >= hoy ? actual.venceEl : hoy;
  const [abierto, setAbierto] = useState(false);
  const [plan, setPlan] = useState(actual?.plan ?? planes[0]?.codigo ?? "");
  const [venceEl, setVenceEl] = useState(sumarDias(base, 30));
  const [esPrueba, setEsPrueba] = useState(false);
  const [referencia, setReferencia] = useState("");
  const [mensaje, setMensaje] = useState<{ ok: boolean; texto: string } | null>(null);
  const [pendiente, iniciar] = useTransition();

  function guardar(e: React.FormEvent) {
    e.preventDefault();
    iniciar(async () => {
      const r = await asignarPlan(vendedor.slug, { plan, venceEl, esPrueba, referencia });
      if (r.ok) {
        setMensaje({ ok: true, texto: "Listo, plan actualizado." });
        setAbierto(false);
      } else {
        setMensaje({ ok: false, texto: Object.values(r.errores)[0] ?? r.mensaje });
      }
    });
  }

  function cancelar() {
    if (!confirm(`¿Cancelar el plan de ${vendedor.nombrePublico}? Se pausan sus autos publicados.`)) return;
    iniciar(async () => {
      const r = await cancelarPlan(vendedor.slug);
      setMensaje(r.ok ? { ok: true, texto: "Plan cancelado." } : { ok: false, texto: r.mensaje });
    });
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        <button type="button" onClick={() => setAbierto((a) => !a)} aria-expanded={abierto} aria-controls={`${id}-form`} className={clasesBoton("oscuro")}>
          {actual ? "Renovar o cambiar plan" : "Asignar plan"}
        </button>
        {actual && (
          <button type="button" onClick={cancelar} disabled={pendiente} className={clasesBoton("suave")}>
            Cancelar plan
          </button>
        )}
      </div>
      {mensaje && (
        <p role={mensaje.ok ? "status" : "alert"} className={`text-sm ${mensaje.ok ? "text-confianza" : "text-marca-oscuro"}`}>
          {mensaje.texto}
        </p>
      )}
      <form id={`${id}-form`} hidden={!abierto} onSubmit={guardar} className="grid gap-3 rounded-2xl bg-fondo p-4 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-sm font-semibold">
          Plan
          <select value={plan} onChange={(e) => setPlan(e.target.value)} className={claseEntrada}>
            {planes.map((p) => (
              <option key={p.codigo} value={p.codigo}>
                {p.nombre} · {precioMensual(p)} · {p.maxPublicaciones} autos
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm font-semibold">
          Vence el (último día incluido)
          <input type="date" required min={hoy} value={venceEl} onChange={(e) => setVenceEl(e.target.value)} className={claseEntrada} />
        </label>
        <label className="flex flex-col gap-1 text-sm font-semibold sm:col-span-2">
          Comprobante o nota
          <input
            value={referencia}
            maxLength={200}
            onChange={(e) => setReferencia(e.target.value)}
            placeholder={esPrueba ? "Prueba ofrecida en la visita del 10/9" : "Transferencia del 10/9, MP 123456"}
            className={claseEntrada}
          />
        </label>
        <label className="flex min-h-11 items-center gap-2.5 text-sm font-semibold">
          <input type="checkbox" checked={esPrueba} onChange={(e) => setEsPrueba(e.target.checked)} className="size-5 accent-[var(--color-tinta)]" />
          Es una prueba gratis
        </label>
        <button type="submit" disabled={pendiente} className={clasesBoton("marca", "md", "font-bold sm:justify-self-end")}>
          {pendiente ? "Guardando…" : "Guardar plan"}
        </button>
      </form>
    </div>
  );
}
