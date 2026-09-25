import Image from "next/image";
import type { TarjetaPublicacion } from "@/lib/api/tipos";
import { CONDICIONES, formatoKm, formatoPrecio } from "@/lib/publicaciones/etiquetas";
import { SiluetaAuto } from "./SiluetaAuto";

/** Tarjeta compacta del stock en el perfil público (grilla de dos columnas del mockup). */
export function TarjetaStock({ publicacion: p }: { publicacion: TarjetaPublicacion }) {
  return (
    <article className="flex flex-col overflow-hidden rounded-[18px] border border-borde bg-superficie">
      <div className="relative flex h-28 items-center justify-center overflow-hidden bg-[#dcd1be]">
        {p.portada ? (
          <Image src={p.portada} alt="" fill sizes="(min-width: 640px) 240px, 50vw" className="object-cover" unoptimized />
        ) : (
          <SiluetaAuto />
        )}
        <span className="absolute top-2 left-2 rounded-md bg-tinta px-2 py-1 text-[11px] font-bold text-fondo">
          {CONDICIONES[p.condicion].toUpperCase()}
        </span>
      </div>
      <div className="flex flex-col gap-0.5 px-3 pt-2.5 pb-3">
        <h3 className="text-[15px] font-bold">
          {p.titulo}
          {p.version && <span className="sr-only"> {p.version}</span>}
        </h3>
        <p className="text-[13px] text-secundario">
          {p.anio} · {formatoKm(p.km)} km
        </p>
        <p className="font-titulo mt-1 text-lg font-extrabold">{formatoPrecio(p.precio, p.moneda)}</p>
      </div>
    </article>
  );
}
