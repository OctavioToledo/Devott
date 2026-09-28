"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { urlDeIngreso } from "@/lib/auth/redireccion";
import { alternarGuardado } from "@/lib/interacciones/acciones";

/**
 * Corazón para guardar una publicación. Cambia al instante y, si la API falla, vuelve atrás.
 * Sin sesión lleva a ingresar y después vuelve a `volverA`.
 */
export function BotonGuardar({
  slug,
  guardado: inicial,
  logueado,
  volverA,
  variante = "tarjeta",
}: {
  slug: string;
  guardado: boolean;
  logueado: boolean;
  volverA: string;
  variante?: "tarjeta" | "detalle";
}) {
  const router = useRouter();
  const [guardado, setGuardado] = useState(inicial);
  const [error, setError] = useState<string | null>(null);
  const [pendiente, iniciar] = useTransition();

  function alternar() {
    if (!logueado) {
      router.push(urlDeIngreso(volverA));
      return;
    }
    const nuevo = !guardado;
    setGuardado(nuevo);
    setError(null);
    iniciar(async () => {
      const r = await alternarGuardado(slug, nuevo);
      if (r.ok) return;
      setGuardado(!nuevo);
      if (r.necesitaIngreso) router.push(urlDeIngreso(volverA));
      else setError(r.mensaje);
    });
  }

  const tarjeta = variante === "tarjeta";
  return (
    <span className="relative z-10 inline-flex">
      <button
        type="button"
        onClick={alternar}
        aria-pressed={guardado}
        aria-label={guardado ? "Quitar de guardados" : "Guardar"}
        title={error ?? undefined}
        data-pendiente={pendiente || undefined}
        className={`flex size-11 items-center justify-center rounded-full transition-transform active:scale-90 ${
          tarjeta ? "bg-superficie/90 shadow-sm backdrop-blur hover:bg-superficie" : "border border-borde-fuerte bg-superficie hover:border-tinta"
        } ${guardado ? "text-marca" : "text-tinta"}`}
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill={guardado ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.9" strokeLinejoin="round" aria-hidden="true">
          <path d="M12 20s-7.5-4.6-7.5-10A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z" />
        </svg>
      </button>
      {error && (
        <span role="alert" className="sr-only">
          {error}
        </span>
      )}
    </span>
  );
}
