"use client";

import Link from "next/link";
import { useEffect, useSyncExternalStore } from "react";

const CLAVE = "devott:ultimo-feed";

/** El feed recuerda su URL (con filtros) en la pestaña, para que el detalle pueda volver a ella. */
export function RecordarFeed({ url }: { url: string }) {
  useEffect(() => {
    try {
      sessionStorage.setItem(CLAVE, url);
    } catch {
      // Sin sessionStorage (modo privado estricto): el botón volverá a la home.
    }
  }, [url]);
  return null;
}

function leerFeedGuardado(): string {
  try {
    const guardado = sessionStorage.getItem(CLAVE);
    return guardado?.startsWith("/") ? guardado : "/";
  } catch {
    return "/";
  }
}

const sinSuscripcion = () => () => {};

/** "Ver más autos": vuelve al último feed visitado en la pestaña, o a la home si se entró por un link. */
export function BotonVolver() {
  // En el servidor no hay sessionStorage: se renderiza la home y se corrige al hidratar.
  const destino = useSyncExternalStore(sinSuscripcion, leerFeedGuardado, () => "/");

  return (
    <Link
      href={destino}
      className="inline-flex min-h-11 items-center gap-1.5 text-sm font-semibold text-secundario no-underline hover:text-tinta"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M15 5l-7 7 7 7" />
      </svg>
      {destino === "/" ? "Ver más autos" : "Volver a los resultados"}
    </Link>
  );
}
