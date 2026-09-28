import Image from "next/image";
import type { MetricasDePublicacion, MiPublicacion } from "@/lib/api/tipos";
import { numeroCompacto } from "@/lib/metricas/formato";
import { formatoKm, formatoPrecio } from "@/lib/publicaciones/etiquetas";
import { AccionesPublicacion } from "./AccionesPublicacion";
import { EstadoBadge } from "./EstadoBadge";
import { SiluetaAuto } from "./SiluetaAuto";

export function ListaMisPublicaciones({
  publicaciones,
  metricas = {},
  dias,
}: {
  publicaciones: MiPublicacion[];
  metricas?: Record<string, MetricasDePublicacion>;
  dias?: number;
}) {
  return (
    <ul className="flex flex-col divide-y divide-[#eee7da] overflow-hidden rounded-[18px] border border-borde bg-superficie">
      {publicaciones.map((p) => (
        <li key={p.id} className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
          <div className="flex min-w-0 items-start gap-3">
            <div className="relative flex h-16 w-[88px] shrink-0 items-center justify-center overflow-hidden rounded-xl bg-[#dcd1be]">
              {p.fotos[0] ? (
                <Image src={p.fotos[0].url} alt="" fill sizes="88px" className="object-cover" unoptimized />
              ) : (
                <SiluetaAuto ancho={64} />
              )}
            </div>
            <div className="flex min-w-0 flex-col gap-1">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="font-bold">
                  {p.titulo}
                  {p.version && <span className="font-normal text-secundario"> {p.version}</span>}
                </h3>
                <EstadoBadge estado={p.estado} />
              </div>
              <p className="text-sm text-secundario">
                {p.anio} · {p.condicion === "0KM" ? "0 km" : `${formatoKm(p.km)} km`} ·{" "}
                {p.fotos.length === 1 ? "1 foto" : `${p.fotos.length} fotos`}
              </p>
              <p className="font-titulo text-lg font-extrabold">{formatoPrecio(p.precio, p.moneda)}</p>
              {dias && p.estado !== "BORRADOR" && <ResumenMetricas m={metricas[p.id]} dias={dias} />}
            </div>
          </div>
          <div className="md:shrink-0">
            <AccionesPublicacion id={p.id} titulo={p.titulo} estado={p.estado} />
          </div>
        </li>
      ))}
    </ul>
  );
}

/** "123 vistas · 4 WhatsApp · 2 guardados" del período elegido en el panel. */
function ResumenMetricas({ m, dias }: { m?: MetricasDePublicacion; dias: number }) {
  const datos = [
    { valor: m?.vistas ?? 0, texto: "vistas", icono: "M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z M12 9.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6z" },
    { valor: m?.contactos ?? 0, texto: "WhatsApp", icono: "M4 20l1.3-3.9A8 8 0 1 1 8 19.2L4 20z" },
    { valor: m?.guardados ?? 0, texto: "guardados", icono: "M12 20s-7.5-4.6-7.5-10A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z" },
  ];
  return (
    <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[13px] text-secundario">
      <span className="sr-only">Últimos {dias} días:</span>
      {datos.map((d) => (
        <span key={d.texto} className="inline-flex items-center gap-1">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinejoin="round" aria-hidden="true">
            <path d={d.icono} />
          </svg>
          <span className="font-semibold text-tinta tabular-nums">{numeroCompacto(d.valor)}</span> {d.texto}
        </span>
      ))}
    </p>
  );
}
