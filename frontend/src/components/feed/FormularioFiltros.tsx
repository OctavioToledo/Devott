"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useState, useTransition } from "react";
import { clasesBoton } from "@/components/ui/boton";
import { claseEntrada } from "@/components/ui/Campo";
import { obtenerModelos } from "@/lib/api/catalogo";
import { pedirApi } from "@/lib/api/cliente";
import type { Marca, Modelo, Moneda, Pagina, TarjetaPublicacion, TipoVendedor } from "@/lib/api/tipos";
import { consultaApi, sinFiltros, urlDelFeed, type FiltrosFeed } from "@/lib/publicaciones/busqueda";
import { CARROCERIAS, COMBUSTIBLES, MONEDAS, numeroConPuntos, TRANSMISIONES } from "@/lib/publicaciones/etiquetas";

const KM_MAXIMOS = [20_000, 50_000, 80_000, 120_000, 200_000];
const PRIMER_ANIO = 1970;

function mostrado(n: number | null): string {
  return n === null ? "" : numeroConPuntos(String(n)).mostrado;
}

/**
 * Panel de filtros. Se edita un borrador y se aplica todo junto con "Ver N autos";
 * mientras tanto se consulta cuántos resultados habría.
 */
export function FormularioFiltros({
  filtros,
  total,
  marcas,
  modelosIniciales,
  alAplicar,
}: {
  filtros: FiltrosFeed;
  total: number;
  marcas: Marca[];
  modelosIniciales: Modelo[];
  alAplicar?: () => void;
}) {
  const router = useRouter();
  const id = useId();
  const [borrador, setBorrador] = useState(filtros);
  const [precioMin, setPrecioMin] = useState(mostrado(filtros.precioMin));
  const [precioMax, setPrecioMax] = useState(mostrado(filtros.precioMax));
  const [modelos, setModelos] = useState(modelosIniciales);
  const [cargandoModelos, setCargandoModelos] = useState(false);
  const [conteo, setConteo] = useState<{ para: string; total: number | null }>({ para: consultaApi(filtros, 0, 1), total });
  const [pendiente, iniciar] = useTransition();

  const consulta = consultaApi(borrador, 0, 1);
  const contando = conteo.para !== consulta;

  // Cuenta los resultados del borrador, con una pausa para no consultar en cada tecla.
  useEffect(() => {
    if (conteo.para === consulta) return;
    const control = new AbortController();
    const espera = setTimeout(async () => {
      try {
        const r = await pedirApi<Pagina<TarjetaPublicacion>>(`/publicaciones?${consulta}`, { signal: control.signal });
        setConteo({ para: consulta, total: r.total });
      } catch {
        if (!control.signal.aborted) setConteo({ para: consulta, total: null });
      }
    }, 350);
    return () => {
      clearTimeout(espera);
      control.abort();
    };
  }, [consulta, conteo.para]);

  function cambiar(cambios: Partial<FiltrosFeed>) {
    setBorrador((b) => ({ ...b, ...cambios }));
  }

  async function elegirMarca(slug: string) {
    cambiar({ marca: slug || null, modelo: null });
    setModelos([]);
    const marca = marcas.find((m) => m.slug === slug);
    if (!marca) return;
    setCargandoModelos(true);
    try {
      setModelos(await obtenerModelos(marca.id));
    } finally {
      setCargandoModelos(false);
    }
  }

  function alternar<T extends string>(lista: T[], valor: T): T[] {
    return lista.includes(valor) ? lista.filter((x) => x !== valor) : [...lista, valor];
  }

  function aplicar(e: React.FormEvent) {
    e.preventDefault();
    let { precioMin: min, precioMax: max, anioMin, anioMax } = borrador;
    if (min !== null && max !== null && min > max) [min, max] = [max, min];
    if (anioMin !== null && anioMax !== null && anioMin > anioMax) [anioMin, anioMax] = [anioMax, anioMin];
    alAplicar?.();
    iniciar(() => router.push(urlDelFeed({ ...borrador, precioMin: min, precioMax: max, anioMin, anioMax }), { scroll: false }));
  }

  function limpiar() {
    setBorrador(sinFiltros(borrador));
    setPrecioMin("");
    setPrecioMax("");
    setModelos([]);
  }

  const anioActual = new Date().getFullYear();
  const anios = Array.from({ length: anioActual + 2 - PRIMER_ANIO }, (_, i) => anioActual + 1 - i);
  const totalMostrado = contando ? null : conteo.total;

  return (
    <form onSubmit={aplicar} className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-1 flex-col gap-6 overflow-y-auto px-4 py-4 lg:px-0">
        <Grupo titulo="Vendedor">
          <Segmentos
            nombre={`${id}-vendedor`}
            opciones={[
              { valor: null, texto: "Todos" },
              { valor: "CONCESIONARIA", texto: "Concesionarias" },
              { valor: "PARTICULAR", texto: "Particulares" },
            ]}
            valor={borrador.tipoVendedor}
            onCambio={(v) => cambiar({ tipoVendedor: v as TipoVendedor | null })}
          />
        </Grupo>

        <Grupo titulo="Marca y modelo">
          <div className="grid gap-2.5">
            <label className="sr-only" htmlFor={`${id}-marca`}>
              Marca
            </label>
            <select id={`${id}-marca`} value={borrador.marca ?? ""} onChange={(e) => elegirMarca(e.target.value)} className={claseEntrada}>
              <option value="">Todas las marcas</option>
              {marcas.map((m) => (
                <option key={m.id} value={m.slug}>
                  {m.nombre}
                </option>
              ))}
            </select>
            <label className="sr-only" htmlFor={`${id}-modelo`}>
              Modelo
            </label>
            <select
              id={`${id}-modelo`}
              value={borrador.modelo ?? ""}
              disabled={!borrador.marca || cargandoModelos}
              onChange={(e) => cambiar({ modelo: e.target.value || null })}
              className={claseEntrada}
            >
              <option value="">
                {cargandoModelos ? "Cargando modelos…" : borrador.marca ? "Todos los modelos" : "Elegí primero la marca"}
              </option>
              {modelos.map((m) => (
                <option key={m.id} value={m.slug}>
                  {m.nombre}
                </option>
              ))}
            </select>
          </div>
        </Grupo>

        <Grupo titulo="Precio">
          <div className="flex flex-col gap-2.5">
            <Segmentos
              nombre={`${id}-moneda`}
              opciones={(["USD", "ARS"] as Moneda[]).map((m) => ({ valor: m, texto: m === "USD" ? "Dólares" : "Pesos" }))}
              valor={borrador.moneda}
              onCambio={(v) => cambiar({ moneda: v as Moneda })}
            />
            <div className="grid grid-cols-2 gap-2.5">
              {(
                [
                  ["Desde", precioMin, setPrecioMin, "precioMin"],
                  ["Hasta", precioMax, setPrecioMax, "precioMax"],
                ] as const
              ).map(([texto, valor, setValor, clave]) => (
                <label key={clave} className="flex flex-col gap-1 text-sm text-secundario">
                  {texto}
                  <span className="relative">
                    <span className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 font-semibold text-tinta">
                      {MONEDAS[borrador.moneda]}
                    </span>
                    <input
                      inputMode="numeric"
                      autoComplete="off"
                      value={valor}
                      placeholder="0"
                      onChange={(e) => {
                        const n = numeroConPuntos(e.target.value);
                        setValor(n.mostrado);
                        cambiar({ [clave]: n.valor === 0 ? null : n.valor });
                      }}
                      className={`${claseEntrada} ${borrador.moneda === "USD" ? "pl-12" : "pl-8"}`}
                    />
                  </span>
                </label>
              ))}
            </div>
          </div>
        </Grupo>

        <Grupo titulo="Año">
          <div className="grid grid-cols-2 gap-2.5">
            {(
              [
                ["Desde", "anioMin"],
                ["Hasta", "anioMax"],
              ] as const
            ).map(([texto, clave]) => (
              <label key={clave} className="flex flex-col gap-1 text-sm text-secundario">
                {texto}
                <select
                  value={borrador[clave] ?? ""}
                  onChange={(e) => cambiar({ [clave]: e.target.value ? Number(e.target.value) : null })}
                  className={claseEntrada}
                >
                  <option value="">Cualquiera</option>
                  {anios.map((a) => (
                    <option key={a} value={a}>
                      {a}
                    </option>
                  ))}
                </select>
              </label>
            ))}
          </div>
        </Grupo>

        <Grupo titulo="Kilómetros">
          <Segmentos
            nombre={`${id}-km`}
            opciones={[
              { valor: null, texto: "Cualquiera" },
              ...KM_MAXIMOS.map((k) => ({ valor: String(k), texto: `Hasta ${k / 1000} mil` })),
            ]}
            valor={borrador.kmMax === null ? null : String(borrador.kmMax)}
            onCambio={(v) => cambiar({ kmMax: v === null ? null : Number(v) })}
          />
        </Grupo>

        <Grupo titulo="Carrocería">
          <Opciones
            etiquetas={CARROCERIAS}
            elegidas={borrador.carroceria}
            onAlternar={(c) => cambiar({ carroceria: alternar(borrador.carroceria, c) })}
          />
        </Grupo>

        <Grupo titulo="Combustible">
          <Opciones
            etiquetas={COMBUSTIBLES}
            elegidas={borrador.combustible}
            onAlternar={(c) => cambiar({ combustible: alternar(borrador.combustible, c) })}
          />
        </Grupo>

        <Grupo titulo="Transmisión">
          <Opciones
            etiquetas={TRANSMISIONES}
            elegidas={borrador.transmision}
            onAlternar={(t) => cambiar({ transmision: alternar(borrador.transmision, t) })}
          />
        </Grupo>

        <Grupo titulo="Condiciones de venta">
          <div className="flex flex-col divide-y divide-borde rounded-2xl border border-borde bg-superficie">
            {(
              [
                ["financia", "Ofrece financiación"],
                ["permuta", "Acepta permuta"],
                ["unicoDueno", "Único dueño"],
              ] as const
            ).map(([clave, texto]) => (
              <label key={clave} className="flex min-h-12 cursor-pointer items-center justify-between gap-3 px-4">
                <span className="text-[15px] font-semibold">{texto}</span>
                <input
                  type="checkbox"
                  role="switch"
                  checked={borrador[clave]}
                  onChange={(e) => cambiar({ [clave]: e.target.checked })}
                  className="peer sr-only"
                />
                <span
                  aria-hidden="true"
                  className="relative h-7 w-12 shrink-0 rounded-full bg-borde-fuerte transition-colors peer-checked:bg-tinta peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-confianza after:absolute after:top-1 after:left-1 after:size-5 after:rounded-full after:bg-white after:transition-transform peer-checked:after:translate-x-5"
                />
              </label>
            ))}
          </div>
        </Grupo>
      </div>

      <div className="sticky bottom-0 flex gap-2 border-t border-borde bg-fondo px-4 py-3 lg:px-0">
        <button type="button" onClick={limpiar} className={clasesBoton("suave", "lg")}>
          Limpiar
        </button>
        <button type="submit" disabled={pendiente} className={clasesBoton("marca", "lg", "flex-1 font-bold")} aria-live="polite">
          {pendiente
            ? "Buscando…"
            : totalMostrado === null
              ? "Ver autos"
              : totalMostrado === 0
                ? "Sin resultados"
                : `Ver ${new Intl.NumberFormat("es-AR").format(totalMostrado)} ${totalMostrado === 1 ? "auto" : "autos"}`}
        </button>
      </div>
    </form>
  );
}

function Grupo({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <fieldset className="flex flex-col">
      <legend className="font-titulo mb-2.5 text-[17px] font-extrabold tracking-[-0.3px]">{titulo}</legend>
      {children}
    </fieldset>
  );
}

const claseChip = (activo: boolean) =>
  `flex min-h-11 cursor-pointer items-center rounded-full border-[1.5px] px-4 text-sm font-semibold select-none has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-confianza ${
    activo ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie text-tinta"
  }`;

/** Una sola opción (radio) con forma de chips. */
function Segmentos({
  nombre,
  opciones,
  valor,
  onCambio,
}: {
  nombre: string;
  opciones: { valor: string | null; texto: string }[];
  valor: string | null;
  onCambio: (valor: string | null) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {opciones.map((o) => (
        <label key={o.texto} className={claseChip(valor === o.valor)}>
          <input
            type="radio"
            name={nombre}
            checked={valor === o.valor}
            onChange={() => onCambio(o.valor)}
            className="sr-only"
          />
          {o.texto}
        </label>
      ))}
    </div>
  );
}

/** Varias opciones (checkbox) con forma de chips. */
function Opciones<T extends string>({
  etiquetas,
  elegidas,
  onAlternar,
}: {
  etiquetas: Record<T, string>;
  elegidas: T[];
  onAlternar: (valor: T) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {(Object.keys(etiquetas) as T[]).map((valor) => (
        <label key={valor} className={claseChip(elegidas.includes(valor))}>
          <input type="checkbox" checked={elegidas.includes(valor)} onChange={() => onAlternar(valor)} className="sr-only" />
          {etiquetas[valor]}
        </label>
      ))}
    </div>
  );
}
