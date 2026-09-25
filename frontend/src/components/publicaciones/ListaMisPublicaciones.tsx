import type { MiPublicacion } from "@/lib/api/tipos";
import { formatoKm, formatoPrecio } from "@/lib/publicaciones/etiquetas";
import { AccionesPublicacion } from "./AccionesPublicacion";
import { EstadoBadge } from "./EstadoBadge";

export function ListaMisPublicaciones({ publicaciones }: { publicaciones: MiPublicacion[] }) {
  return (
    <ul className="flex flex-col divide-y divide-[#eee7da] overflow-hidden rounded-[18px] border border-borde bg-superficie">
      {publicaciones.map((p) => (
        <li key={p.id} className="flex flex-col gap-3 p-4 md:flex-row md:items-center md:justify-between">
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
              {p.cantidadFotos === 1 ? "1 foto" : `${p.cantidadFotos} fotos`}
            </p>
            <p className="font-titulo text-lg font-extrabold">{formatoPrecio(p.precio, p.moneda)}</p>
          </div>
          <div className="md:shrink-0">
            <AccionesPublicacion id={p.id} titulo={p.titulo} estado={p.estado} />
          </div>
        </li>
      ))}
    </ul>
  );
}
