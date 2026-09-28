"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { clasesBoton, type TamanoBoton } from "@/components/ui/boton";
import { urlDeIngreso } from "@/lib/auth/redireccion";
import { alternarSeguimiento } from "@/lib/interacciones/acciones";

/** Seguir / Siguiendo. Mismo comportamiento que el corazón: optimista y con vuelta atrás si falla. */
export function BotonSeguir({
  vendedorSlug,
  siguiendo: inicial,
  logueado,
  volverA,
  tamano = "md",
  className = "",
}: {
  vendedorSlug: string;
  siguiendo: boolean;
  logueado: boolean;
  volverA: string;
  tamano?: TamanoBoton;
  className?: string;
}) {
  const router = useRouter();
  const [siguiendo, setSiguiendo] = useState(inicial);
  const [error, setError] = useState<string | null>(null);
  const [, iniciar] = useTransition();

  function alternar() {
    if (!logueado) {
      router.push(urlDeIngreso(volverA));
      return;
    }
    const nuevo = !siguiendo;
    setSiguiendo(nuevo);
    setError(null);
    iniciar(async () => {
      const r = await alternarSeguimiento(vendedorSlug, nuevo);
      if (r.ok) return;
      setSiguiendo(!nuevo);
      if (r.necesitaIngreso) router.push(urlDeIngreso(volverA));
      else setError(r.mensaje);
    });
  }

  return (
    <span className={`relative z-10 inline-flex flex-col ${className}`}>
      <button
        type="button"
        onClick={alternar}
        aria-pressed={siguiendo}
        className={clasesBoton(siguiendo ? "suave" : "oscuro", tamano, "w-full font-bold")}
      >
        {siguiendo ? (
          <>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 12.5l4.5 4.5L19 7.5" />
            </svg>
            Siguiendo
          </>
        ) : (
          "Seguir"
        )}
      </button>
      {error && (
        <span role="alert" className="mt-1 text-xs text-marca-oscuro">
          {error}
        </span>
      )}
    </span>
  );
}
