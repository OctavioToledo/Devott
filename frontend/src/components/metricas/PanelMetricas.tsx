import Link from "next/link";
import type { Metricas } from "@/lib/api/tipos";
import { METRICAS, numeroCompacto, PERIODOS, variacion, type ClaveMetrica, type Periodo } from "@/lib/metricas/formato";
import { GraficoDiario } from "./GraficoDiario";

/** Resumen del período: cuatro indicadores con su variación y el gráfico por día. */
export function PanelMetricas({ metricas, periodo }: { metricas: Metricas; periodo: Periodo }) {
  const claves = Object.keys(METRICAS) as ClaveMetrica[];
  return (
    <section aria-labelledby="titulo-metricas" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="titulo-metricas" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
          Cómo te va
        </h2>
        <nav aria-label="Período" className="flex gap-1 rounded-full border border-borde-fuerte bg-superficie p-1">
          {PERIODOS.map((p) => (
            <Link
              key={p}
              href={p === 7 ? "/panel" : `/panel?dias=${p}`}
              scroll={false}
              aria-current={p === periodo ? "page" : undefined}
              className={`flex min-h-9 items-center rounded-full px-3.5 text-sm font-semibold no-underline ${
                p === periodo ? "bg-tinta text-fondo" : "text-tinta hover:bg-fondo"
              }`}
            >
              {p} días
            </Link>
          ))}
        </nav>
      </div>

      <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {claves.map((c) => (
          <li key={c}>
            <Indicador clave={c} metricas={metricas} />
          </li>
        ))}
      </ul>

      <GraficoDiario key={periodo} serie={metricas.serie} />
    </section>
  );
}

function Indicador({ clave, metricas }: { clave: ClaveMetrica; metricas: Metricas }) {
  const actual = metricas.actual[clave];
  const cambio = variacion(actual, metricas.anterior[clave], metricas.dias);
  const valores = metricas.serie.map((d) => d[clave]);
  return (
    <div className="flex h-full flex-col gap-1 rounded-[18px] border border-borde bg-superficie p-4">
      <p className="text-sm text-secundario">{METRICAS[clave].titulo}</p>
      <p className="font-titulo text-[30px] leading-none font-extrabold tracking-[-0.8px]">{numeroCompacto(actual)}</p>
      <p className="flex items-center gap-1 text-[13px] font-semibold">
        {cambio.cambio && (
          <>
            <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true" className={cambio.direccion === "sube" ? "text-confianza" : "text-secundario"}>
              <path d={cambio.direccion === "sube" ? "M6 2l4 6H2z" : "M6 10L2 4h8z"} fill="currentColor" />
            </svg>
            <span className={cambio.direccion === "sube" ? "text-confianza" : "text-secundario"}>{cambio.cambio}</span>
          </>
        )}
        <span className="font-normal text-secundario">{cambio.detalle}</span>
      </p>
      <Sparkline valores={valores} />
    </div>
  );
}

/** Tendencia mínima dentro del indicador; los valores exactos están en el gráfico y su tabla. */
function Sparkline({ valores }: { valores: number[] }) {
  if (valores.length < 2) return null;
  const max = Math.max(1, ...valores);
  const ancho = 100;
  const alto = 24;
  const puntos = valores.map((v, i) => [(i / (valores.length - 1)) * ancho, alto - 2 - (v / max) * (alto - 4)]);
  return (
    <svg viewBox={`0 0 ${ancho} ${alto}`} preserveAspectRatio="none" className="mt-auto h-6 w-full pt-1" aria-hidden="true">
      <polyline
        points={puntos.map((p) => p.join(",")).join(" ")}
        fill="none"
        stroke="var(--color-grafico)"
        strokeWidth="2"
        vectorEffect="non-scaling-stroke"
        strokeLinejoin="round"
      />
    </svg>
  );
}
