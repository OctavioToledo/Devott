"use client";

import Image from "next/image";
import { useId, useRef, useState } from "react";
import { Boton } from "@/components/ui/Boton";
import { clasesBoton } from "@/components/ui/boton";
import type { Foto, MiPublicacion } from "@/lib/api/tipos";
import { comprimirImagen } from "@/lib/imagenes/comprimir";
import { subirArchivo } from "@/lib/imagenes/subir";
import { confirmarFoto, eliminarFoto, pedirSubidaFoto, reordenarFotos } from "@/lib/publicaciones/acciones";

type Subida = { clave: string; nombre: string; progreso: number; error?: string };

/**
 * Carga, orden y borrado de las fotos de una publicación. Cada foto se comprime en el navegador,
 * se sube directo al almacenamiento con una URL firmada y después se confirma en la API.
 */
export function GestorFotos({ publicacion }: { publicacion: MiPublicacion }) {
  const idEntrada = useId();
  const entrada = useRef<HTMLInputElement>(null);
  const [fotos, setFotos] = useState<Foto[]>(publicacion.fotos);
  const [subidas, setSubidas] = useState<Subida[]>([]);
  const [ocupado, setOcupado] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [anuncio, setAnuncio] = useState("");

  const lugares = publicacion.maxFotos - fotos.length;

  function actualizarSubida(clave: string, cambios: Partial<Subida>) {
    setSubidas((actuales) => actuales.map((s) => (s.clave === clave ? { ...s, ...cambios } : s)));
  }

  async function subirUna(archivo: File, clave: string): Promise<boolean> {
    try {
      const imagen = await comprimirImagen(archivo);
      const pedido = await pedirSubidaFoto(publicacion.id, imagen.tipo);
      if (!pedido.ok) throw new Error(pedido.mensaje);
      await subirArchivo(pedido.valor, imagen.archivo, (p) => actualizarSubida(clave, { progreso: p }));
      const confirmacion = await confirmarFoto(publicacion.id, pedido.valor.ruta, imagen.ancho, imagen.alto);
      if (!confirmacion.ok) throw new Error(confirmacion.mensaje);
      setFotos(confirmacion.valor.fotos);
      setSubidas((actuales) => actuales.filter((s) => s.clave !== clave));
      return true;
    } catch (e) {
      actualizarSubida(clave, { error: e instanceof Error ? e.message : "No pudimos subir esta foto." });
      return false;
    }
  }

  async function elegirArchivos(lista: FileList | null) {
    if (!lista || lista.length === 0) return;
    setAviso(null);
    const archivos = Array.from(lista).filter((a) => a.type.startsWith("image/"));
    const aSubir = archivos.slice(0, Math.max(0, lugares));
    if (archivos.length > aSubir.length) {
      setAviso(`Tu plan permite hasta ${publicacion.maxFotos} fotos por publicación. Subimos las primeras ${aSubir.length}.`);
    }
    const nuevas = aSubir.map((a, i) => ({ clave: `${Date.now()}-${i}`, nombre: a.name, progreso: 0 }));
    setSubidas((actuales) => [...actuales.filter((s) => !s.error), ...nuevas]);
    setOcupado(true);
    let subidasOk = 0;
    // De a una, para respetar el orden en que se eligieron y no saturar conexiones móviles.
    for (let i = 0; i < aSubir.length; i++) {
      if (await subirUna(aSubir[i], nuevas[i].clave)) subidasOk++;
    }
    setOcupado(false);
    setAnuncio(subidasOk === 1 ? "Se subió 1 foto." : `Se subieron ${subidasOk} fotos.`);
    if (entrada.current) entrada.current.value = "";
  }

  async function mover(indice: number, delta: -1 | 1) {
    const destino = indice + delta;
    if (destino < 0 || destino >= fotos.length) return;
    const nuevoOrden = [...fotos];
    [nuevoOrden[indice], nuevoOrden[destino]] = [nuevoOrden[destino], nuevoOrden[indice]];
    const anteriores = fotos;
    setFotos(nuevoOrden);
    setOcupado(true);
    const resultado = await reordenarFotos(publicacion.id, nuevoOrden.map((f) => f.id));
    setOcupado(false);
    if (resultado.ok) {
      setFotos(resultado.valor.fotos);
      setAnuncio(`Foto movida a la posición ${destino + 1}.`);
    } else {
      setFotos(anteriores);
      setAviso(resultado.mensaje);
    }
  }

  async function quitar(foto: Foto) {
    setAviso(null);
    setOcupado(true);
    const resultado = await eliminarFoto(publicacion.id, foto.id);
    setOcupado(false);
    if (resultado.ok) {
      setFotos(resultado.valor.fotos);
      setAnuncio("Foto eliminada.");
    } else {
      setAviso(resultado.mensaje);
    }
  }

  return (
    <section aria-labelledby="titulo-fotos" className="flex flex-col gap-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="titulo-fotos" className="font-titulo text-xl font-extrabold tracking-[-0.4px]">
          Fotos
        </h2>
        <span className="text-sm text-secundario">
          {fotos.length} de {publicacion.maxFotos}
        </span>
      </div>
      <p className="text-sm text-secundario">
        La primera es la portada. Sacalas de día, con el auto limpio y desde varios ángulos.
      </p>

      <p className="sr-only" aria-live="polite">
        {anuncio}
      </p>
      {aviso && (
        <p role="alert" className="rounded-2xl bg-celeste p-3 text-sm text-confianza-profundo">
          {aviso}
        </p>
      )}

      {fotos.length > 0 && (
        <ol className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {fotos.map((foto, i) => (
            <li key={foto.id} className="flex flex-col overflow-hidden rounded-2xl border border-borde bg-superficie">
              <div className="relative aspect-[4/3] bg-fondo">
                <Image
                  src={foto.url}
                  alt={`Foto ${i + 1}`}
                  fill
                  sizes="(min-width: 640px) 200px, 50vw"
                  className="object-cover"
                  unoptimized
                />
                {i === 0 && (
                  <span className="absolute top-2 left-2 rounded-md bg-tinta px-2 py-1 text-[11px] font-bold text-fondo">
                    PORTADA
                  </span>
                )}
              </div>
              <div className="flex justify-between gap-1 p-1.5">
                <div className="flex gap-1">
                  <BotonIcono etiqueta={`Mover la foto ${i + 1} antes`} disabled={ocupado || i === 0} onClick={() => mover(i, -1)}>
                    <path d="M15 6l-6 6 6 6" />
                  </BotonIcono>
                  <BotonIcono
                    etiqueta={`Mover la foto ${i + 1} después`}
                    disabled={ocupado || i === fotos.length - 1}
                    onClick={() => mover(i, 1)}
                  >
                    <path d="M9 6l6 6-6 6" />
                  </BotonIcono>
                </div>
                <BotonIcono etiqueta={`Eliminar la foto ${i + 1}`} disabled={ocupado} onClick={() => quitar(foto)}>
                  <path d="M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12" />
                </BotonIcono>
              </div>
            </li>
          ))}
        </ol>
      )}

      {subidas.length > 0 && (
        <ul className="flex flex-col gap-2">
          {subidas.map((s) => (
            <li key={s.clave} className="rounded-2xl border border-borde bg-superficie p-3 text-sm">
              <div className="flex justify-between gap-2">
                <span className="truncate">{s.nombre}</span>
                <span className={s.error ? "font-semibold text-marca-oscuro" : "text-secundario"}>
                  {s.error ? "Falló" : `${Math.round(s.progreso * 100)}%`}
                </span>
              </div>
              {s.error ? (
                <p className="mt-1 text-marca-oscuro">{s.error}</p>
              ) : (
                <div className="mt-2 h-1.5 rounded-full bg-fondo" aria-hidden="true">
                  <div className="h-1.5 rounded-full bg-marca transition-[width]" style={{ width: `${s.progreso * 100}%` }} />
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {lugares > 0 ? (
        <div>
          <input
            ref={entrada}
            id={idEntrada}
            type="file"
            accept="image/*"
            multiple
            disabled={ocupado}
            onChange={(e) => elegirArchivos(e.target.files)}
            className="peer sr-only"
          />
          <label
            htmlFor={idEntrada}
            className={clasesBoton(
              fotos.length === 0 ? "marca" : "suave",
              "lg",
              `peer-focus-visible:outline-2 peer-focus-visible:outline-confianza ${ocupado ? "pointer-events-none opacity-50" : ""}`,
            )}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M4 8h3l2-3h6l2 3h3v11H4z" />
              <circle cx="12" cy="13" r="3.5" />
            </svg>
            {ocupado ? "Subiendo…" : fotos.length === 0 ? "Agregar fotos" : "Agregar más fotos"}
          </label>
        </div>
      ) : (
        <p className="text-sm text-secundario">Llegaste al máximo de fotos de tu plan.</p>
      )}
    </section>
  );
}

function BotonIcono({
  etiqueta,
  disabled,
  onClick,
  children,
}: {
  etiqueta: string;
  disabled?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <Boton
      variante="suave"
      aria-label={etiqueta}
      title={etiqueta}
      disabled={disabled}
      onClick={onClick}
      className="size-11 border-transparent px-0!"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        {children}
      </svg>
    </Boton>
  );
}
