"use client";

import { useState } from "react";
import { clasesBoton, type TamanoBoton, type VarianteBoton } from "./boton";

export function BotonCopiar({
  texto,
  etiqueta = "Copiar link",
  variante = "contorno",
  tamano = "md",
  className,
}: {
  texto: string;
  etiqueta?: string;
  variante?: VarianteBoton;
  tamano?: TamanoBoton;
  className?: string;
}) {
  const [copiado, setCopiado] = useState(false);

  async function copiar() {
    try {
      await navigator.clipboard.writeText(texto);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      // Sin permiso de portapapeles: se muestra el texto para copiarlo a mano.
      window.prompt("Copiá el link:", texto);
    }
  }

  return (
    <button type="button" onClick={copiar} className={clasesBoton(variante, tamano, className)}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1" />
        <path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1" />
      </svg>
      <span aria-live="polite">{copiado ? "¡Copiado!" : etiqueta}</span>
    </button>
  );
}
