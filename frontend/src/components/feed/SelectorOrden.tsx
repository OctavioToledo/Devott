"use client";

import { useRouter } from "next/navigation";
import { useTransition } from "react";
import type { OrdenBusqueda } from "@/lib/api/tipos";
import { ORDENES, urlDelFeed, type FiltrosFeed } from "@/lib/publicaciones/busqueda";

export function SelectorOrden({ filtros }: { filtros: FiltrosFeed }) {
  const router = useRouter();
  const [pendiente, iniciar] = useTransition();
  const opciones = (Object.keys(ORDENES) as OrdenBusqueda[]).filter((o) => o !== "CERCANIA" || filtros.zona);

  return (
    <label className="flex items-center gap-2 text-sm text-secundario">
      <span className="shrink-0">Ordenar por</span>
      <select
        value={filtros.orden}
        disabled={pendiente}
        onChange={(e) =>
          iniciar(() => router.push(urlDelFeed({ ...filtros, orden: e.target.value as OrdenBusqueda }), { scroll: false }))
        }
        className="min-h-11 cursor-pointer rounded-full border border-borde-fuerte bg-superficie px-3 font-semibold text-tinta"
      >
        {opciones.map((o) => (
          <option key={o} value={o}>
            {ORDENES[o]}
          </option>
        ))}
      </select>
    </label>
  );
}
