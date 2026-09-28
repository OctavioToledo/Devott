"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import type { Foto } from "@/lib/api/tipos";
import { SiluetaAuto } from "./SiluetaAuto";

/**
 * Fotos de la publicación: carrusel que se desliza con el dedo (scroll-snap), con flechas en desktop
 * y contador. Tocar una foto la abre a pantalla completa.
 */
export function Galeria({ fotos, titulo }: { fotos: Foto[]; titulo: string }) {
  const carrusel = useRef<HTMLUListElement>(null);
  const [actual, setActual] = useState(0);
  const [ampliada, setAmpliada] = useState<number | null>(null);

  if (fotos.length === 0) {
    return (
      <div className="flex aspect-[4/3] items-center justify-center bg-[#dcd1be] sm:rounded-3xl">
        <SiluetaAuto ancho={180} />
      </div>
    );
  }

  function alDeslizar() {
    const el = carrusel.current;
    if (el) setActual(Math.round(el.scrollLeft / el.clientWidth));
  }

  function irA(i: number) {
    const el = carrusel.current;
    if (!el) return;
    const destino = (i + fotos.length) % fotos.length;
    el.scrollTo({ left: destino * el.clientWidth, behavior: "smooth" });
  }

  return (
    <div className="relative">
      <ul
        ref={carrusel}
        onScroll={alDeslizar}
        aria-label={`Fotos de ${titulo}`}
        className="flex aspect-[4/3] snap-x snap-mandatory overflow-x-auto overscroll-x-contain bg-[#dcd1be] [scrollbar-width:none] sm:rounded-3xl [&::-webkit-scrollbar]:hidden"
      >
        {fotos.map((f, i) => (
          <li key={f.id} className="relative h-full w-full shrink-0 snap-center">
            <button
              type="button"
              onClick={() => setAmpliada(i)}
              aria-label={`Ampliar foto ${i + 1} de ${fotos.length}`}
              className="relative block h-full w-full cursor-zoom-in"
            >
              <Image
                src={f.url}
                alt=""
                fill
                sizes="(min-width: 1024px) 720px, 100vw"
                className="object-cover"
                priority={i === 0}
                loading={i === 0 ? undefined : "lazy"}
                unoptimized
              />
            </button>
          </li>
        ))}
      </ul>
      {fotos.length > 1 && (
        <>
          <span aria-hidden="true" className="absolute right-3 bottom-3 rounded-full bg-tinta/80 px-2.5 py-1 text-xs font-semibold text-fondo tabular-nums">
            {actual + 1}/{fotos.length}
          </span>
          <Flecha lado="izquierda" onClick={() => irA(actual - 1)} className="absolute top-1/2 left-3 hidden -translate-y-1/2 sm:flex" />
          <Flecha lado="derecha" onClick={() => irA(actual + 1)} className="absolute top-1/2 right-3 hidden -translate-y-1/2 sm:flex" />
        </>
      )}
      {ampliada !== null && (
        <Ampliada fotos={fotos} inicial={ampliada} onCerrar={(i) => { setAmpliada(null); irA(i); }} />
      )}
    </div>
  );
}

function Flecha({ lado, onClick, className = "", oscura = false }: { lado: "izquierda" | "derecha"; onClick: () => void; className?: string; oscura?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={lado === "izquierda" ? "Foto anterior" : "Foto siguiente"}
      className={`size-11 items-center justify-center rounded-full ${oscura ? "bg-white/15 text-white hover:bg-white/25" : "bg-superficie/90 text-tinta shadow hover:bg-superficie"} ${className}`}
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d={lado === "izquierda" ? "M15 5l-7 7 7 7" : "M9 5l7 7-7 7"} />
      </svg>
    </button>
  );
}

/** Foto a pantalla completa, con flechas, teclado y contador. Al cerrar, el carrusel queda en la misma foto. */
function Ampliada({ fotos, inicial, onCerrar }: { fotos: Foto[]; inicial: number; onCerrar: (actual: number) => void }) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [i, setI] = useState(inicial);
  const inicioToque = useRef<number | null>(null);
  const mover = (paso: number) => setI((x) => (x + paso + fotos.length) % fotos.length);

  useEffect(() => {
    dialogo.current?.showModal();
  }, []);

  const foto = fotos[i];
  return (
    <dialog
      ref={dialogo}
      aria-label={`Foto ${i + 1} de ${fotos.length}`}
      onClose={() => onCerrar(i)}
      onKeyDown={(e) => {
        if (e.key === "ArrowLeft") mover(-1);
        if (e.key === "ArrowRight") mover(1);
      }}
      onTouchStart={(e) => (inicioToque.current = e.touches[0].clientX)}
      onTouchEnd={(e) => {
        if (inicioToque.current === null) return;
        const delta = e.changedTouches[0].clientX - inicioToque.current;
        inicioToque.current = null;
        if (Math.abs(delta) > 50) mover(delta < 0 ? 1 : -1);
      }}
      className="m-0 h-dvh max-h-none w-full max-w-none bg-black p-0 text-white backdrop:bg-black"
    >
      <div className="relative flex h-full items-center justify-center">
        <Image src={foto.url} alt="" fill sizes="100vw" className="object-contain" unoptimized />
        <button
          type="button"
          onClick={() => dialogo.current?.close()}
          aria-label="Cerrar"
          className="absolute top-3 right-3 flex size-11 items-center justify-center rounded-full bg-white/15 hover:bg-white/25"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
        {fotos.length > 1 && (
          <>
            <Flecha lado="izquierda" oscura onClick={() => mover(-1)} className="absolute top-1/2 left-3 flex -translate-y-1/2" />
            <Flecha lado="derecha" oscura onClick={() => mover(1)} className="absolute top-1/2 right-3 flex -translate-y-1/2" />
            <span className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-white/15 px-3 py-1 text-sm tabular-nums">
              {i + 1}/{fotos.length}
            </span>
          </>
        )}
      </div>
    </dialog>
  );
}
