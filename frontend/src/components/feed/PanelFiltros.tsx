"use client";

import { useRef } from "react";
import type { Marca, Modelo } from "@/lib/api/tipos";
import type { FiltrosFeed } from "@/lib/publicaciones/busqueda";
import { FormularioFiltros } from "./FormularioFiltros";

type Props = { filtros: FiltrosFeed; total: number; marcas: Marca[]; modelos: Modelo[]; cantidad: number };

/** En mobile: botón "Filtros" que abre una hoja a pantalla completa. */
export function BotonFiltros({ filtros, total, marcas, modelos, cantidad }: Props) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const cerrar = () => dialogo.current?.close();

  return (
    <>
      <button
        type="button"
        onClick={() => dialogo.current?.showModal()}
        className="flex min-h-11 shrink-0 items-center gap-2 rounded-full border-[1.5px] border-tinta bg-superficie px-4 text-sm font-semibold lg:hidden"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
          <path d="M4 7h10M18 7h2M4 17h4M12 17h8" />
          <circle cx="16" cy="7" r="2" />
          <circle cx="10" cy="17" r="2" />
        </svg>
        Filtros
        {cantidad > 0 && (
          <span className="flex size-5 items-center justify-center rounded-full bg-marca text-[11px] font-bold text-white">
            {cantidad}
            <span className="sr-only"> aplicados</span>
          </span>
        )}
      </button>
      <dialog
        ref={dialogo}
        aria-labelledby="titulo-filtros"
        onClick={(e) => e.target === e.currentTarget && cerrar()}
        className="m-0 h-dvh max-h-none w-full max-w-none bg-fondo p-0 text-tinta backdrop:bg-tinta/40 sm:mx-auto sm:mt-6 sm:h-[calc(100dvh-3rem)] sm:max-w-lg sm:rounded-3xl"
      >
        <div className="flex h-full flex-col">
          <div className="flex items-center justify-between border-b border-borde px-4 py-2">
            <h2 id="titulo-filtros" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
              Filtros
            </h2>
            <button type="button" onClick={cerrar} aria-label="Cerrar filtros" className="flex size-11 items-center justify-center rounded-full hover:bg-borde">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>
          <FormularioFiltros filtros={filtros} total={total} marcas={marcas} modelosIniciales={modelos} alAplicar={cerrar} />
        </div>
      </dialog>
    </>
  );
}

/** En desktop: los filtros quedan siempre a la vista en una columna. */
export function BarraFiltros({ filtros, total, marcas, modelos }: Omit<Props, "cantidad">) {
  return (
    <aside aria-labelledby="titulo-barra-filtros" className="sticky top-4 hidden max-h-[calc(100dvh-2rem)] flex-col lg:flex">
      <h2 id="titulo-barra-filtros" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
        Filtros
      </h2>
      <FormularioFiltros filtros={filtros} total={total} marcas={marcas} modelosIniciales={modelos} />
    </aside>
  );
}
