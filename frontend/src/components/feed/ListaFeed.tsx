"use client";

import { useState } from "react";
import { clasesBoton } from "@/components/ui/boton";
import { ErrorApi, pedirApi } from "@/lib/api/cliente";
import type { Pagina, TarjetaPublicacion } from "@/lib/api/tipos";
import { consultaApi, type FiltrosFeed } from "@/lib/publicaciones/busqueda";
import { TarjetaFeed } from "./TarjetaFeed";

/**
 * Grilla del feed. La primera página viene renderizada del servidor; "Ver más" trae las siguientes
 * desde el navegador. Cuando cambian los filtros, la página se remonta con una `key` distinta.
 */
export function ListaFeed({
  filtros,
  inicial,
  guardados,
  logueado,
  volverA,
}: {
  filtros: FiltrosFeed;
  inicial: Pagina<TarjetaPublicacion>;
  guardados: string[];
  logueado: boolean;
  volverA: string;
}) {
  const [items, setItems] = useState(inicial.items);
  const [pagina, setPagina] = useState(inicial.pagina);
  const [hayMas, setHayMas] = useState(inicial.hayMas);
  const [estado, setEstado] = useState<"quieto" | "cargando" | "error">("quieto");

  async function verMas() {
    setEstado("cargando");
    try {
      const siguiente = await pedirApi<Pagina<TarjetaPublicacion>>(`/publicaciones?${consultaApi(filtros, pagina + 1)}`);
      // Si entre páginas se publicó algo, puede venir repetido: se descarta.
      setItems((actuales) => {
        const vistos = new Set(actuales.map((p) => p.slug));
        return [...actuales, ...siguiente.items.filter((p) => !vistos.has(p.slug))];
      });
      setPagina(siguiente.pagina);
      setHayMas(siguiente.hayMas);
      setEstado("quieto");
    } catch (e) {
      setEstado(e instanceof ErrorApi ? "error" : "quieto");
    }
  }

  return (
    <>
      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((p, i) => (
          <li key={p.slug}>
            <TarjetaFeed
              publicacion={p}
              prioridad={i < 2}
              guardado={guardados.includes(p.slug)}
              logueado={logueado}
              volverA={volverA}
            />
          </li>
        ))}
      </ul>
      {estado === "error" && (
        <p role="alert" className="text-center text-sm text-marca-oscuro">
          No pudimos traer más autos. Probá de nuevo.
        </p>
      )}
      {hayMas && (
        <button
          type="button"
          onClick={verMas}
          disabled={estado === "cargando"}
          className={clasesBoton("suave", "lg", "mx-auto w-full max-w-xs font-bold")}
        >
          {estado === "cargando" ? "Cargando…" : "Ver más autos"}
        </button>
      )}
    </>
  );
}
