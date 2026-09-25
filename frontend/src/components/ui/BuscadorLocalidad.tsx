"use client";

import { useEffect, useId, useRef, useState } from "react";
import { ErrorApi, pedirApi } from "@/lib/api/cliente";
import type { Localidad } from "@/lib/api/tipos";
import { claseEntrada } from "./Campo";

export type LocalidadElegida = { ciudad: string; provincia: string; lat: number; lng: number };

const etiquetaDe = (l: { ciudad: string; provincia: string }) => `${l.ciudad}, ${l.provincia}`;

/**
 * Combobox accesible que busca localidades en la API mientras se escribe.
 * Solo se considera elegida una localidad de la lista (trae sus coordenadas).
 */
export function BuscadorLocalidad({
  id,
  valor,
  onCambio,
  invalido,
  describidoPor,
}: {
  id: string;
  valor: LocalidadElegida | null;
  onCambio: (localidad: LocalidadElegida | null) => void;
  invalido?: boolean;
  describidoPor?: string;
}) {
  const idLista = useId();
  const [texto, setTexto] = useState(valor ? etiquetaDe(valor) : "");
  const [opciones, setOpciones] = useState<Localidad[]>([]);
  const [abierto, setAbierto] = useState(false);
  const [activa, setActiva] = useState(-1);
  const [estado, setEstado] = useState<"quieto" | "buscando" | "error">("quieto");
  const buscado = useRef("");

  useEffect(() => {
    const consulta = texto.trim();
    if (consulta.length < 2 || (valor && texto === etiquetaDe(valor)) || consulta === buscado.current) return;

    const control = new AbortController();
    const espera = setTimeout(async () => {
      setEstado("buscando");
      try {
        const resultado = await pedirApi<Localidad[]>(`/ubicaciones?q=${encodeURIComponent(consulta)}`, {
          signal: control.signal,
        });
        buscado.current = consulta;
        setOpciones(resultado);
        setActiva(resultado.length ? 0 : -1);
        setAbierto(true);
        setEstado("quieto");
      } catch (e) {
        if (control.signal.aborted) return;
        setEstado(e instanceof ErrorApi ? "error" : "quieto");
      }
    }, 250);
    return () => {
      clearTimeout(espera);
      control.abort();
    };
  }, [texto, valor]);

  function elegir(localidad: Localidad) {
    const elegida = { ciudad: localidad.nombre, provincia: localidad.provincia, lat: localidad.lat, lng: localidad.lng };
    onCambio(elegida);
    setTexto(etiquetaDe(elegida));
    setAbierto(false);
  }

  function alEscribir(nuevo: string) {
    setTexto(nuevo);
    if (valor) onCambio(null);
    if (nuevo.trim().length < 2) {
      setOpciones([]);
      setAbierto(false);
      buscado.current = "";
    }
  }

  function alPresionarTecla(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!abierto || opciones.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiva((i) => (i + 1) % opciones.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiva((i) => (i - 1 + opciones.length) % opciones.length);
    } else if (e.key === "Enter" && activa >= 0) {
      e.preventDefault();
      elegir(opciones[activa]);
    } else if (e.key === "Escape") {
      setAbierto(false);
    }
  }

  const mostrarLista = abierto && texto.trim().length >= 2;

  return (
    <div className="relative">
      <input
        id={id}
        type="text"
        role="combobox"
        autoComplete="off"
        aria-autocomplete="list"
        aria-expanded={mostrarLista}
        aria-controls={idLista}
        aria-activedescendant={mostrarLista && activa >= 0 ? `${idLista}-${activa}` : undefined}
        aria-invalid={invalido || undefined}
        aria-describedby={describidoPor}
        placeholder="Escribí tu ciudad o localidad"
        value={texto}
        onChange={(e) => alEscribir(e.target.value)}
        onKeyDown={alPresionarTecla}
        onFocus={() => opciones.length > 0 && !valor && setAbierto(true)}
        onBlur={() => setAbierto(false)}
        className={claseEntrada}
      />
      {estado === "buscando" && (
        <span className="absolute top-3.5 right-4 text-sm text-secundario" aria-hidden="true">
          Buscando…
        </span>
      )}
      <ul
        id={idLista}
        role="listbox"
        aria-label="Localidades"
        hidden={!mostrarLista}
        className="absolute z-10 mt-1 max-h-72 w-full overflow-auto rounded-2xl border border-borde-fuerte bg-superficie py-1 shadow-lg"
      >
        {opciones.length === 0 ? (
          <li className="px-4 py-3 text-sm text-secundario">No encontramos esa localidad.</li>
        ) : (
          opciones.map((opcion, i) => (
            <li
              key={opcion.id}
              id={`${idLista}-${i}`}
              role="option"
              aria-selected={i === activa}
              // mousedown en lugar de click: si no, el blur del input cierra la lista antes.
              onMouseDown={(e) => {
                e.preventDefault();
                elegir(opcion);
              }}
              onMouseEnter={() => setActiva(i)}
              className={`flex min-h-11 cursor-pointer flex-col justify-center px-4 py-2 ${i === activa ? "bg-fondo" : ""}`}
            >
              <span className="font-semibold">{opcion.nombre}</span>
              <span className="text-sm text-secundario">{opcion.provincia}</span>
            </li>
          ))
        )}
      </ul>
      {estado === "error" && (
        <p role="alert" className="mt-1.5 text-sm text-marca-oscuro">
          No pudimos buscar localidades. Probá de nuevo en un rato.
        </p>
      )}
    </div>
  );
}
