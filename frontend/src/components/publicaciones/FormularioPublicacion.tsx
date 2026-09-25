"use client";

import { useRef, useState, useTransition } from "react";
import { Boton } from "@/components/ui/Boton";
import { BuscadorLocalidad } from "@/components/ui/BuscadorLocalidad";
import { Campo, claseEntrada, describir } from "@/components/ui/Campo";
import { obtenerModelos } from "@/lib/api/catalogo";
import type { Condicion, Marca, MiPublicacion, Modelo, Moneda, PublicacionRequest } from "@/lib/api/tipos";
import { guardarPublicacion, type ResultadoGuardadoPublicacion } from "@/lib/publicaciones/acciones";
import {
  CARROCERIAS,
  COMBUSTIBLES,
  CONDICIONES,
  MONEDAS,
  TRACCIONES,
  TRANSMISIONES,
  numeroConPuntos,
} from "@/lib/publicaciones/etiquetas";

const ANIO_MAXIMO = new Date().getFullYear() + 1;
const ANIOS = Array.from({ length: ANIO_MAXIMO - 1950 + 1 }, (_, i) => ANIO_MAXIMO - i);

function datosIniciales(inicial?: MiPublicacion): PublicacionRequest {
  return {
    modeloId: inicial?.modelo.modeloId ?? null,
    version: inicial?.version ?? "",
    anio: inicial?.anio ?? null,
    km: inicial?.km ?? null,
    condicion: inicial?.condicion ?? null,
    precio: inicial?.precio ?? null,
    moneda: inicial?.moneda ?? "USD",
    carroceria: inicial?.carroceria ?? null,
    combustible: inicial?.combustible ?? null,
    transmision: inicial?.transmision ?? null,
    traccion: inicial?.traccion ?? null,
    color: inicial?.color ?? "",
    puertas: inicial?.puertas ?? null,
    financia: inicial?.financia ?? false,
    aceptaPermuta: inicial?.aceptaPermuta ?? false,
    unicoDueno: inicial?.unicoDueno ?? false,
    descripcion: inicial?.descripcion ?? "",
    localidad:
      inicial?.localidad.ciudad && inicial.localidad.provincia && inicial.localidad.lat !== null && inicial.localidad.lng !== null
        ? {
            ciudad: inicial.localidad.ciudad,
            provincia: inicial.localidad.provincia,
            lat: inicial.localidad.lat,
            lng: inicial.localidad.lng,
          }
        : null,
  };
}

export function FormularioPublicacion({
  marcas,
  inicial,
  modelosIniciales = [],
  ubicacionDelPerfil,
}: {
  marcas: Marca[];
  inicial?: MiPublicacion;
  modelosIniciales?: Modelo[];
  ubicacionDelPerfil: string;
}) {
  const [datos, setDatos] = useState<PublicacionRequest>(() => datosIniciales(inicial));
  const [marcaId, setMarcaId] = useState<number | null>(inicial?.modelo.marcaId ?? null);
  const [modelos, setModelos] = useState<Modelo[]>(modelosIniciales);
  const [cargandoModelos, setCargandoModelos] = useState(false);
  const [precioMostrado, setPrecioMostrado] = useState(inicial ? numeroConPuntos(String(inicial.precio)).mostrado : "");
  const [kmMostrado, setKmMostrado] = useState(inicial ? numeroConPuntos(String(inicial.km)).mostrado : "");
  // En una publicación nueva se usa la ubicación del perfil. Al editar, la que tenga guardada.
  const [otraUbicacion, setOtraUbicacion] = useState(false);
  const [resultado, setResultado] = useState<ResultadoGuardadoPublicacion | null>(null);
  const [enviando, iniciarEnvio] = useTransition();
  const resumen = useRef<HTMLDivElement>(null);
  const errores = resultado?.errores ?? {};

  function cambiar<K extends keyof PublicacionRequest>(campo: K, valor: PublicacionRequest[K]) {
    setDatos((actual) => ({ ...actual, [campo]: valor }));
  }

  async function elegirMarca(id: number | null) {
    setMarcaId(id);
    cambiar("modeloId", null);
    setModelos([]);
    if (id === null) return;
    setCargandoModelos(true);
    try {
      setModelos(await obtenerModelos(id));
    } finally {
      setCargandoModelos(false);
    }
  }

  function elegirCondicion(condicion: Condicion) {
    setDatos((actual) => ({ ...actual, condicion, km: condicion === "0KM" && actual.km === null ? 0 : actual.km }));
    if (condicion === "0KM" && datos.km === null) setKmMostrado("0");
  }

  function enviar(e: React.FormEvent) {
    e.preventDefault();
    const cuerpo: PublicacionRequest = { ...datos, localidad: otraUbicacion || inicial ? datos.localidad : null };
    iniciarEnvio(async () => {
      const r = await guardarPublicacion(inicial?.id ?? null, cuerpo);
      setResultado(r);
      requestAnimationFrame(() => resumen.current?.focus());
    });
  }

  const ubicacionActual = datos.localidad
    ? `${datos.localidad.ciudad}, ${datos.localidad.provincia}`
    : ubicacionDelPerfil;

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-8">
      {resultado && (
        <div ref={resumen} tabIndex={-1} role="alert" className="rounded-2xl bg-celeste p-4 text-sm text-confianza-profundo outline-none">
          <p className="font-semibold">{resultado.mensaje}</p>
          {Object.keys(errores).length > 0 && <p>Revisá los campos marcados.</p>}
        </div>
      )}

      <Seccion titulo="El vehículo">
        <div className="grid gap-5 sm:grid-cols-2">
          <Campo id="marca" etiqueta="Marca" error={errores.modeloId && !marcaId ? errores.modeloId : undefined}>
            <select
              id="marca"
              value={marcaId ?? ""}
              onChange={(e) => elegirMarca(e.target.value ? Number(e.target.value) : null)}
              className={claseEntrada}
            >
              <option value="">Elegí la marca</option>
              {marcas.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nombre}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="modeloId" etiqueta="Modelo" error={marcaId ? errores.modeloId : undefined}>
            <select
              id="modeloId"
              value={datos.modeloId ?? ""}
              disabled={!marcaId || cargandoModelos}
              onChange={(e) => cambiar("modeloId", e.target.value ? Number(e.target.value) : null)}
              aria-invalid={!!errores.modeloId || undefined}
              aria-describedby={describir("modeloId", undefined, marcaId ? errores.modeloId : undefined)}
              className={`${claseEntrada} disabled:opacity-60`}
            >
              <option value="">{cargandoModelos ? "Cargando modelos…" : marcaId ? "Elegí el modelo" : "Primero elegí la marca"}</option>
              {modelos.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nombre}
                </option>
              ))}
            </select>
          </Campo>
        </div>

        <Campo id="version" etiqueta="Versión" opcional ayuda="Por ejemplo: 2.8 SRV 4x4 AT" error={errores.version}>
          <input
            id="version"
            type="text"
            maxLength={80}
            value={datos.version}
            onChange={(e) => cambiar("version", e.target.value)}
            aria-invalid={!!errores.version || undefined}
            aria-describedby={describir("version", true, errores.version)}
            className={claseEntrada}
          />
        </Campo>

        <fieldset className="flex flex-col gap-2" aria-describedby={errores.condicion ? "condicion-error" : undefined}>
          <legend className="mb-1.5 text-sm font-semibold">Condición</legend>
          <div className="flex gap-2">
            {(Object.keys(CONDICIONES) as Condicion[]).map((c) => (
              <OpcionPastilla
                key={c}
                tipo="radio"
                nombre="condicion"
                marcada={datos.condicion === c}
                onCambio={() => elegirCondicion(c)}
              >
                {CONDICIONES[c]}
              </OpcionPastilla>
            ))}
          </div>
          {errores.condicion && (
            <p id="condicion-error" className="text-sm font-semibold text-marca-oscuro">
              {errores.condicion}
            </p>
          )}
        </fieldset>

        <div className="grid gap-5 sm:grid-cols-2">
          <Campo id="anio" etiqueta="Año" error={errores.anio}>
            <select
              id="anio"
              value={datos.anio ?? ""}
              onChange={(e) => cambiar("anio", e.target.value ? Number(e.target.value) : null)}
              aria-invalid={!!errores.anio || undefined}
              aria-describedby={describir("anio", undefined, errores.anio)}
              className={claseEntrada}
            >
              <option value="">Elegí el año</option>
              {ANIOS.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="km" etiqueta="Kilómetros" error={errores.km}>
            <input
              id="km"
              type="text"
              inputMode="numeric"
              value={kmMostrado}
              onChange={(e) => {
                const { valor, mostrado } = numeroConPuntos(e.target.value);
                setKmMostrado(mostrado);
                cambiar("km", valor);
              }}
              aria-invalid={!!errores.km || undefined}
              aria-describedby={describir("km", undefined, errores.km)}
              className={claseEntrada}
            />
          </Campo>
        </div>
      </Seccion>

      <Seccion titulo="Precio">
        <Campo id="precio" etiqueta="Precio" error={errores.precio ?? errores.moneda}>
          <div className="flex gap-2">
            <div role="radiogroup" aria-label="Moneda" className="flex shrink-0 rounded-2xl border border-borde-fuerte bg-superficie p-1">
              {(Object.keys(MONEDAS) as Moneda[]).map((m) => (
                <button
                  key={m}
                  type="button"
                  role="radio"
                  aria-checked={datos.moneda === m}
                  onClick={() => cambiar("moneda", m)}
                  className={`min-h-10 min-w-12 rounded-xl px-3 text-sm font-semibold ${datos.moneda === m ? "bg-tinta text-fondo" : "text-tinta"}`}
                >
                  {MONEDAS[m]}
                </button>
              ))}
            </div>
            <input
              id="precio"
              type="text"
              inputMode="numeric"
              placeholder={datos.moneda === "USD" ? "34.900" : "27.450.000"}
              value={precioMostrado}
              onChange={(e) => {
                const { valor, mostrado } = numeroConPuntos(e.target.value);
                setPrecioMostrado(mostrado);
                cambiar("precio", valor);
              }}
              aria-invalid={!!errores.precio || undefined}
              aria-describedby={describir("precio", undefined, errores.precio ?? errores.moneda)}
              className={claseEntrada}
            />
          </div>
        </Campo>
        <div className="flex flex-wrap gap-2">
          <OpcionPastilla tipo="checkbox" marcada={datos.financia} onCambio={(v) => cambiar("financia", v)}>
            Financia
          </OpcionPastilla>
          <OpcionPastilla tipo="checkbox" marcada={datos.aceptaPermuta} onCambio={(v) => cambiar("aceptaPermuta", v)}>
            Acepta permuta
          </OpcionPastilla>
        </div>
      </Seccion>

      <Seccion titulo="Características">
        <div className="grid gap-5 sm:grid-cols-2">
          <SelectEnum id="carroceria" etiqueta="Carrocería" opciones={CARROCERIAS} valor={datos.carroceria} error={errores.carroceria} onCambio={(v) => cambiar("carroceria", v)} />
          <SelectEnum id="combustible" etiqueta="Combustible" opciones={COMBUSTIBLES} valor={datos.combustible} error={errores.combustible} onCambio={(v) => cambiar("combustible", v)} />
          <SelectEnum id="transmision" etiqueta="Transmisión" opciones={TRANSMISIONES} valor={datos.transmision} error={errores.transmision} onCambio={(v) => cambiar("transmision", v)} />
          <SelectEnum id="traccion" etiqueta="Tracción" opcional opciones={TRACCIONES} valor={datos.traccion} error={errores.traccion} onCambio={(v) => cambiar("traccion", v)} />
          <Campo id="puertas" etiqueta="Puertas" opcional error={errores.puertas}>
            <select
              id="puertas"
              value={datos.puertas ?? ""}
              onChange={(e) => cambiar("puertas", e.target.value ? Number(e.target.value) : null)}
              aria-invalid={!!errores.puertas || undefined}
              aria-describedby={describir("puertas", undefined, errores.puertas)}
              className={claseEntrada}
            >
              <option value="">Sin indicar</option>
              {[2, 3, 4, 5].map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="color" etiqueta="Color" opcional error={errores.color}>
            <input
              id="color"
              type="text"
              maxLength={40}
              value={datos.color}
              onChange={(e) => cambiar("color", e.target.value)}
              aria-invalid={!!errores.color || undefined}
              aria-describedby={describir("color", undefined, errores.color)}
              className={claseEntrada}
            />
          </Campo>
        </div>
        <div className="flex flex-wrap gap-2">
          <OpcionPastilla tipo="checkbox" marcada={datos.unicoDueno} onCambio={(v) => cambiar("unicoDueno", v)}>
            Único dueño
          </OpcionPastilla>
        </div>
      </Seccion>

      <Seccion titulo="Descripción">
        <Campo id="descripcion" etiqueta="Contá los detalles" opcional error={errores.descripcion}>
          <textarea
            id="descripcion"
            rows={5}
            maxLength={3000}
            placeholder="Estado general, service al día, equipamiento, si tiene algún detalle…"
            value={datos.descripcion}
            onChange={(e) => cambiar("descripcion", e.target.value)}
            aria-invalid={!!errores.descripcion || undefined}
            aria-describedby={describir("descripcion", undefined, errores.descripcion)}
            className={`${claseEntrada} py-3`}
          />
        </Campo>
      </Seccion>

      <Seccion titulo="Ubicación">
        {otraUbicacion ? (
          <Campo id="localidad" etiqueta="Localidad del vehículo" error={errores.localidad}>
            <BuscadorLocalidad
              id="localidad"
              valor={datos.localidad}
              onCambio={(l) => cambiar("localidad", l)}
              invalido={!!errores.localidad}
              describidoPor={describir("localidad", undefined, errores.localidad)}
            />
          </Campo>
        ) : (
          <div className="flex flex-col items-start gap-2 rounded-2xl border border-borde bg-superficie p-4 sm:flex-row sm:items-center sm:justify-between">
            <p>
              <span className="text-sm text-secundario">Se publica en </span>
              <span className="font-semibold">{ubicacionActual}</span>
            </p>
            <Boton variante="suave" onClick={() => setOtraUbicacion(true)}>
              Cambiar ubicación
            </Boton>
          </div>
        )}
      </Seccion>

      <Boton type="submit" variante="marca" tamano="lg" disabled={enviando} className="w-full sm:w-auto sm:self-start">
        {enviando ? "Guardando…" : inicial ? "Guardar cambios" : "Guardar y seguir con las fotos"}
      </Boton>
    </form>
  );
}

function Seccion({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-5">
      <h2 className="font-titulo text-xl font-extrabold tracking-[-0.4px]">{titulo}</h2>
      {children}
    </section>
  );
}

/** Radio o checkbox con forma de pastilla, como los chips del mockup. */
function OpcionPastilla({
  tipo,
  nombre,
  marcada,
  onCambio,
  children,
}: {
  tipo: "radio" | "checkbox";
  nombre?: string;
  marcada: boolean;
  onCambio: (marcada: boolean) => void;
  children: React.ReactNode;
}) {
  return (
    <label
      className={`flex min-h-11 cursor-pointer items-center gap-2 rounded-full border-[1.5px] px-4 text-sm font-semibold has-focus-visible:outline-2 has-focus-visible:outline-confianza ${
        marcada ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie text-tinta"
      }`}
    >
      <input
        type={tipo}
        name={nombre}
        checked={marcada}
        onChange={(e) => onCambio(e.target.checked)}
        className="sr-only"
      />
      {children}
    </label>
  );
}

function SelectEnum<T extends string>({
  id,
  etiqueta,
  opciones,
  valor,
  error,
  opcional = false,
  onCambio,
}: {
  id: string;
  etiqueta: string;
  opciones: Record<T, string>;
  valor: T | null;
  error?: string;
  opcional?: boolean;
  onCambio: (valor: T | null) => void;
}) {
  return (
    <Campo id={id} etiqueta={etiqueta} opcional={opcional} error={error}>
      <select
        id={id}
        value={valor ?? ""}
        onChange={(e) => onCambio((e.target.value || null) as T | null)}
        aria-invalid={!!error || undefined}
        aria-describedby={describir(id, undefined, error)}
        className={claseEntrada}
      >
        <option value="">{opcional ? "Sin indicar" : "Elegí una opción"}</option>
        {(Object.entries(opciones) as [T, string][]).map(([codigo, texto]) => (
          <option key={codigo} value={codigo}>
            {texto}
          </option>
        ))}
      </select>
    </Campo>
  );
}
