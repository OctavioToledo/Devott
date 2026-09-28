import Image from "next/image";
import Link from "next/link";
import { BotonGuardar } from "@/components/interacciones/BotonGuardar";
import { Odometro } from "@/components/publicaciones/Odometro";
import { SiluetaAuto } from "@/components/publicaciones/SiluetaAuto";
import type { TarjetaPublicacion } from "@/lib/api/tipos";
import { formatoDistancia } from "@/lib/publicaciones/busqueda";
import { CONDICIONES, formatoPrecio } from "@/lib/publicaciones/etiquetas";

/** Tarjeta del feed: foto, precio, año, odómetro y dónde está. Toda la tarjeta lleva al detalle. */
export function TarjetaFeed({
  publicacion: p,
  prioridad = false,
  guardado,
  logueado,
  volverA,
}: {
  publicacion: TarjetaPublicacion;
  prioridad?: boolean;
  guardado: boolean;
  logueado: boolean;
  volverA: string;
}) {
  const lugar = [p.ciudad, p.provincia].filter(Boolean).join(", ");
  return (
    <article className="group relative flex h-full flex-col overflow-hidden rounded-[20px] border border-borde bg-superficie">
      <div className="relative flex aspect-[4/3] items-center justify-center overflow-hidden bg-[#dcd1be]">
        {p.portada ? (
          <Image
            src={p.portada}
            alt=""
            fill
            sizes="(min-width: 1024px) 360px, (min-width: 640px) 50vw, 100vw"
            className="object-cover transition-transform duration-300 group-hover:scale-[1.02]"
            priority={prioridad}
            unoptimized
          />
        ) : (
          <SiluetaAuto />
        )}
        <div className="absolute top-2.5 left-2.5 flex gap-1.5">
          <span className="rounded-md bg-tinta px-2 py-1 text-[11px] font-bold text-fondo">
            {CONDICIONES[p.condicion].toUpperCase()}
          </span>
          {p.financia && (
            <span className="rounded-md bg-celeste px-2 py-1 text-[11px] font-bold text-confianza-profundo">
              FINANCIA
            </span>
          )}
        </div>
        <div className="absolute top-1.5 right-1.5">
          <BotonGuardar slug={p.slug} guardado={guardado} logueado={logueado} volverA={volverA} />
        </div>
        {p.cantidadFotos > 1 && (
          <span className="absolute right-2.5 bottom-2.5 rounded-md bg-tinta/80 px-2 py-1 text-[11px] font-semibold text-fondo">
            {p.cantidadFotos} fotos
          </span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-1.5 px-3.5 pt-3 pb-3.5">
        <p className="font-titulo text-[22px] leading-none font-extrabold tracking-[-0.5px]">
          {formatoPrecio(p.precio, p.moneda)}
        </p>
        <h3 className="text-base leading-snug font-bold">
          <Link
            href={`/publicaciones/${p.slug}`}
            className="no-underline after:absolute after:inset-0 after:content-[''] focus-visible:outline-none"
          >
            {p.titulo}
          </Link>
          {p.version && <span className="block truncate text-sm font-normal text-secundario">{p.version}</span>}
        </h3>
        <div className="flex items-center gap-2.5 text-sm text-secundario">
          <span className="font-semibold text-tinta">{p.anio}</span>
          <Odometro km={p.km} />
        </div>
        {(lugar || p.distanciaKm != null) && (
          <p className="mt-auto truncate pt-1 text-[13px] text-secundario">
            {lugar}
            {p.distanciaKm != null && (
              <span className="font-semibold text-tinta">
                {lugar ? " · " : ""}
                {formatoDistancia(p.distanciaKm)}
              </span>
            )}
          </p>
        )}
      </div>
      {/* Foco visible para toda la tarjeta cuando se navega con teclado. */}
      <span aria-hidden="true" className="pointer-events-none absolute inset-0 rounded-[20px] ring-confianza group-has-[a:focus-visible]:ring-2" />
    </article>
  );
}
