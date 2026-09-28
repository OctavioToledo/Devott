import { digitosOdometro, formatoKm } from "@/lib/publicaciones/etiquetas";

/** Kilometraje como odómetro: dígitos en cajas oscuras. */
export function Odometro({ km, className = "" }: { km: number; className?: string }) {
  return (
    <span className={`inline-flex items-center gap-1 ${className}`}>
      <span className="sr-only">{formatoKm(km)} km</span>
      <span aria-hidden="true" className="inline-flex gap-[2px]">
        {digitosOdometro(km).map((d, i) => (
          <span
            key={i}
            className="flex h-[22px] w-[15px] items-center justify-center rounded-[4px] bg-tinta font-mono text-[13px] font-bold text-fondo tabular-nums"
          >
            {d}
          </span>
        ))}
      </span>
      <span aria-hidden="true" className="text-xs font-semibold text-secundario">
        km
      </span>
    </span>
  );
}
