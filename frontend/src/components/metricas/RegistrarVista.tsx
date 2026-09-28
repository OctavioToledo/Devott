"use client";

import { useEffect } from "react";
import { urlDeLaApi } from "@/lib/config";

const CLAVE_VISITANTE = "devott:visitante";

/** Id anónimo y al azar de este navegador. Sin almacenamiento disponible, uno nuevo por visita. */
function idDeVisitante(): string {
  try {
    const guardado = localStorage.getItem(CLAVE_VISITANTE);
    if (guardado && /^[A-Za-z0-9-]{16,64}$/.test(guardado)) return guardado;
    const nuevo = crypto.randomUUID();
    localStorage.setItem(CLAVE_VISITANTE, nuevo);
    return nuevo;
  } catch {
    return crypto.randomUUID();
  }
}

/**
 * Cuenta la vista de un perfil o de una publicación para las métricas del vendedor. Se manda desde el
 * navegador (no desde el servidor) para no contar bots, prefetch ni caché; la API deduplica por día.
 */
export function RegistrarVista({ tipo, slug }: { tipo: "VISTA_PERFIL" | "VISTA_PUBLICACION"; slug: string }) {
  useEffect(() => {
    if (navigator.webdriver) return;
    fetch(`${urlDeLaApi()}/eventos/vista`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ tipo, slug, visitante: idDeVisitante() }),
      keepalive: true,
    }).catch(() => {
      // Una vista perdida no es un error para quien navega.
    });
  }, [tipo, slug]);
  return null;
}
