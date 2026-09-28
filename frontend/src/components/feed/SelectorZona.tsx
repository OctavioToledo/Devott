"use client";

import { useRouter } from "next/navigation";
import { useId, useState, useTransition } from "react";
import { clasesBoton } from "@/components/ui/boton";
import { BuscadorLocalidad, type LocalidadElegida } from "@/components/ui/BuscadorLocalidad";
import {
  RADIO_POR_DEFECTO,
  RADIOS_KM,
  urlDelFeed,
  type FiltrosFeed,
  type Zona,
} from "@/lib/publicaciones/busqueda";

type EstadoUbicacion = "quieto" | "buscando" | "denegada" | "error";

/** Botón con la zona actual que despliega el buscador de localidad, "Usar mi ubicación" y el radio. */
export function SelectorZona({ filtros }: { filtros: FiltrosFeed }) {
  const router = useRouter();
  const idPanel = useId();
  const idLocalidad = useId();
  const [abierto, setAbierto] = useState(false);
  const [localidad, setLocalidad] = useState<LocalidadElegida | null>(null);
  const [radioKm, setRadioKm] = useState(filtros.zona?.radioKm ?? RADIO_POR_DEFECTO);
  const [ubicacion, setUbicacion] = useState<EstadoUbicacion>("quieto");
  const [pendiente, iniciar] = useTransition();

  function aplicar(zona: Zona | null) {
    // Con zona nueva conviene ver primero lo más cercano; sin zona no se puede ordenar por cercanía.
    let orden = filtros.orden;
    if (zona && !filtros.zona && orden === "RECIENTES") orden = "CERCANIA";
    if (!zona && orden === "CERCANIA") orden = "RECIENTES";
    setAbierto(false);
    iniciar(() => router.push(urlDelFeed({ ...filtros, zona, orden }), { scroll: false }));
  }

  function usarMiUbicacion() {
    if (!("geolocation" in navigator)) {
      setUbicacion("error");
      return;
    }
    setUbicacion("buscando");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setUbicacion("quieto");
        // Tres decimales (~100 m) alcanzan para buscar por zona y no dejan la ubicación exacta en la URL.
        const redondear = (n: number) => Math.round(n * 1000) / 1000;
        aplicar({ nombre: "Tu ubicación", lat: redondear(pos.coords.latitude), lng: redondear(pos.coords.longitude), radioKm });
      },
      (error) => setUbicacion(error.code === error.PERMISSION_DENIED ? "denegada" : "error"),
      { enableHighAccuracy: false, timeout: 10_000, maximumAge: 10 * 60_000 },
    );
  }

  function aplicarElegida() {
    if (localidad) {
      aplicar({ nombre: `${localidad.ciudad}, ${localidad.provincia}`, lat: localidad.lat, lng: localidad.lng, radioKm });
    } else if (filtros.zona) {
      aplicar({ ...filtros.zona, radioKm });
    }
  }

  const zona = filtros.zona;
  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        aria-expanded={abierto}
        aria-controls={idPanel}
        onClick={() => setAbierto((a) => !a)}
        className="flex min-h-12 w-full items-center gap-2.5 rounded-2xl border border-borde-fuerte bg-superficie px-4 text-left"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="shrink-0 text-marca">
          <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z" />
          <circle cx="12" cy="9.5" r="2.5" />
        </svg>
        <span className="min-w-0 flex-1">
          <span className="block text-xs font-semibold text-secundario">{zona ? "Buscando cerca de" : "Zona"}</span>
          <span className="block truncate font-semibold">
            {zona ? `${zona.nombre} · ${zona.radioKm} km` : "Toda la Argentina"}
          </span>
        </span>
        <span className="text-sm font-semibold text-marca">{pendiente ? "Buscando…" : "Cambiar"}</span>
      </button>

      <div id={idPanel} hidden={!abierto} className="flex flex-col gap-3.5 rounded-2xl border border-borde bg-superficie p-4">
        <button
          type="button"
          onClick={usarMiUbicacion}
          disabled={ubicacion === "buscando"}
          className={clasesBoton("suave", "md", "justify-start")}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
            <circle cx="12" cy="12" r="7" />
          </svg>
          {ubicacion === "buscando" ? "Buscando tu ubicación…" : "Usar mi ubicación"}
        </button>
        {ubicacion === "denegada" && (
          <p role="alert" className="text-sm text-secundario">
            No tenemos permiso para ver tu ubicación. Elegí tu localidad acá abajo.
          </p>
        )}
        {ubicacion === "error" && (
          <p role="alert" className="text-sm text-secundario">
            No pudimos obtener tu ubicación. Elegí tu localidad acá abajo.
          </p>
        )}

        <div className="flex flex-col gap-1.5">
          <label htmlFor={idLocalidad} className="text-sm font-semibold">
            O elegí una localidad
          </label>
          <BuscadorLocalidad id={idLocalidad} valor={localidad} onCambio={setLocalidad} />
        </div>

        <fieldset className="flex flex-col gap-1.5">
          <legend className="mb-1.5 text-sm font-semibold">Distancia</legend>
          <div className="flex flex-wrap gap-2">
            {RADIOS_KM.map((r) => (
              <label
                key={r}
                className={`flex min-h-11 cursor-pointer items-center rounded-full border-[1.5px] px-4 text-sm font-semibold has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-confianza ${
                  radioKm === r ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie"
                }`}
              >
                <input type="radio" name="radio" value={r} checked={radioKm === r} onChange={() => setRadioKm(r)} className="sr-only" />
                {r} km
              </label>
            ))}
          </div>
        </fieldset>

        <div className="flex gap-2">
          {zona && (
            <button type="button" onClick={() => aplicar(null)} className={clasesBoton("suave", "lg", "flex-1")}>
              Toda la Argentina
            </button>
          )}
          <button
            type="button"
            onClick={aplicarElegida}
            disabled={!localidad && !zona}
            className={clasesBoton("oscuro", "lg", "flex-1 font-bold")}
          >
            Buscar en esta zona
          </button>
        </div>
      </div>
    </div>
  );
}
