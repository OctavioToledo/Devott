"use client";

import { useId, useState } from "react";
import type { MetricasDelDia } from "@/lib/api/tipos";
import { conUnidad, fechaCorta, fechaDiaMes, METRICAS, numeroCompacto, type ClaveMetrica } from "@/lib/metricas/formato";

/**
 * Barras por día de una métrica a elección. Una sola serie, un solo color: el título la nombra.
 * Cada barra muestra su valor al pasar el mouse o con el teclado, y hay una tabla para lectores de pantalla.
 */
export function GraficoDiario({ serie }: { serie: MetricasDelDia[] }) {
  const id = useId();
  const [clave, setClave] = useState<ClaveMetrica>("vistasPublicaciones");
  const [activa, setActiva] = useState<number | null>(null);

  const valores = serie.map((d) => d[clave]);
  const maximo = Math.max(1, ...valores);
  const total = valores.reduce((a, b) => a + b, 0);
  const denso = serie.length > 31;
  const etiquetasX = [0, Math.floor((serie.length - 1) / 2), serie.length - 1];

  return (
    <section aria-labelledby={`${id}-titulo`} className="flex flex-col gap-4 rounded-[18px] border border-borde bg-superficie p-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 id={`${id}-titulo`} className="font-bold">
          {METRICAS[clave].titulo} por día
        </h3>
        <p className="text-sm text-secundario">{conUnidad(total, clave)} en total</p>
      </div>

      <div role="radiogroup" aria-label="Métrica del gráfico" className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
        {(Object.keys(METRICAS) as ClaveMetrica[]).map((c) => (
          <label
            key={c}
            className={`flex min-h-11 shrink-0 cursor-pointer items-center rounded-full border-[1.5px] px-3.5 text-sm font-semibold has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-confianza ${
              c === clave ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie"
            }`}
          >
            <input type="radio" name={`${id}-metrica`} checked={c === clave} onChange={() => setClave(c)} className="sr-only" />
            {METRICAS[c].titulo}
          </label>
        ))}
      </div>

      <div className="relative">
        {/* Eje Y mínimo: el máximo arriba y la base. Grilla recesiva. */}
        <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 top-0 flex items-center gap-2 text-[11px] text-secundario">
          <span className="tabular-nums">{numeroCompacto(maximo)}</span>
          <span className="h-px flex-1 border-t border-dashed border-borde" />
        </div>
        <ol
          aria-label={`${METRICAS[clave].titulo} por día`}
          className={`flex h-40 items-end pt-5 ${denso ? "gap-px" : "gap-[2px]"}`}
          onPointerLeave={() => setActiva(null)}
        >
          {serie.map((d, i) => {
            const valor = d[clave];
            const alto = valor === 0 ? 0 : Math.max(3, (valor / maximo) * 100);
            return (
              <li
                key={d.fecha}
                tabIndex={0}
                onPointerEnter={() => setActiva(i)}
                onFocus={() => setActiva(i)}
                onBlur={() => setActiva(null)}
                aria-label={`${fechaCorta(d.fecha)}: ${conUnidad(valor, clave)}`}
                className="group relative flex h-full flex-1 cursor-default items-end focus-visible:outline-2 focus-visible:outline-confianza"
              >
                <span
                  className={`block w-full rounded-t-[4px] transition-opacity ${activa !== null && activa !== i ? "opacity-60" : ""}`}
                  style={{ height: `${alto}%`, background: "var(--color-grafico)" }}
                />
              </li>
            );
          })}
        </ol>
        <div aria-hidden="true" className="h-px bg-borde-fuerte" />
        <div aria-hidden="true" className="mt-1.5 flex justify-between text-[11px] text-secundario">
          {etiquetasX.map((i) => (
            <span key={i}>{fechaDiaMes(serie[i].fecha)}</span>
          ))}
        </div>
        {activa !== null && (
          <div
            aria-hidden="true"
            className="pointer-events-none absolute top-0 z-10 -translate-x-1/2 rounded-xl border border-borde bg-superficie px-3 py-2 text-sm shadow-md"
            style={{ left: `${((activa + 0.5) / serie.length) * 100}%` }}
          >
            <p className="font-bold whitespace-nowrap tabular-nums">{conUnidad(serie[activa][clave], clave)}</p>
            <p className="whitespace-nowrap text-xs text-secundario">{fechaCorta(serie[activa].fecha)}</p>
          </div>
        )}
      </div>

      <table className="sr-only">
        <caption>{METRICAS[clave].titulo} por día</caption>
        <thead>
          <tr>
            <th scope="col">Día</th>
            <th scope="col">{METRICAS[clave].titulo}</th>
          </tr>
        </thead>
        <tbody>
          {serie.map((d) => (
            <tr key={d.fecha}>
              <td>{fechaCorta(d.fecha)}</td>
              <td>{d[clave]}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
