"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { Boton } from "@/components/ui/Boton";
import { BuscadorLocalidad } from "@/components/ui/BuscadorLocalidad";
import { Campo, claseEntrada, describir } from "@/components/ui/Campo";
import type { MiVendedor, TipoVendedor, VendedorRequest } from "@/lib/api/tipos";
import { consultarSlug, guardarPerfil, type ResultadoGuardado } from "@/lib/vendedores/acciones";
import { limpiarSlugMientrasSeEscribe, slugDesde, urlVisibleDelPerfil } from "@/lib/vendedores/formato";

type EstadoSlug =
  | { tipo: "vacio" }
  | { tipo: "consultando" }
  | { tipo: "disponible" }
  | { tipo: "ocupado"; motivo: string }
  | { tipo: "desconocido" };

function datosIniciales(inicial?: MiVendedor): VendedorRequest {
  return {
    tipo: inicial?.tipo ?? null,
    nombrePublico: inicial?.nombrePublico ?? "",
    slug: inicial?.slug ?? "",
    whatsapp: inicial?.whatsapp ?? "",
    telefono: inicial?.telefono ?? "",
    descripcion: inicial?.descripcion ?? "",
    direccion: inicial?.direccion ?? "",
    localidad:
      inicial && inicial.localidad.lat !== null && inicial.localidad.lng !== null
        ? {
            ciudad: inicial.localidad.ciudad,
            provincia: inicial.localidad.provincia,
            lat: inicial.localidad.lat,
            lng: inicial.localidad.lng,
          }
        : null,
    horarios: inicial?.horarios ?? "",
    instagram: inicial?.instagram ? `@${inicial.instagram}` : "",
    facebook: inicial?.facebook ?? "",
  };
}

const TIPOS: { valor: TipoVendedor; titulo: string; detalle: string }[] = [
  { valor: "CONCESIONARIA", titulo: "Concesionaria", detalle: "Agencia, concesionario o venta de usados" },
  { valor: "PARTICULAR", titulo: "Particular", detalle: "Vendo mi auto o unos pocos" },
];

export function FormularioPerfil({ modo, inicial }: { modo: "alta" | "edicion"; inicial?: MiVendedor }) {
  const [datos, setDatos] = useState<VendedorRequest>(() => datosIniciales(inicial));
  const [slugEditado, setSlugEditado] = useState(modo === "edicion");
  const [consultaSlug, setConsultaSlug] = useState<{ slug: string; estado: EstadoSlug } | null>(null);
  const [resultado, setResultado] = useState<ResultadoGuardado | null>(null);
  const [enviando, iniciarEnvio] = useTransition();
  const resumen = useRef<HTMLDivElement>(null);

  const errores = resultado?.errores ?? {};

  function cambiar<K extends keyof VendedorRequest>(campo: K, valor: VendedorRequest[K]) {
    setDatos((actual) => ({ ...actual, [campo]: valor }));
  }

  function cambiarNombre(nombre: string) {
    setDatos((actual) => ({ ...actual, nombrePublico: nombre, slug: slugEditado ? actual.slug : slugDesde(nombre) }));
  }

  // Consulta en vivo si el link está libre. El resultado se guarda junto al slug consultado,
  // así mientras se escribe se muestra "revisando" sin actualizar estado dentro del efecto.
  const slugLimpio = datos.slug.replace(/-+$/, "");
  const esSlugPropio = slugLimpio === inicial?.slug;
  const estadoSlug: EstadoSlug =
    slugLimpio.length < 3
      ? { tipo: "vacio" }
      : esSlugPropio
        ? { tipo: "disponible" }
        : consultaSlug?.slug === slugLimpio
          ? consultaSlug.estado
          : { tipo: "consultando" };

  useEffect(() => {
    if (slugLimpio.length < 3 || esSlugPropio) return;
    let vigente = true;
    const espera = setTimeout(async () => {
      const respuesta = await consultarSlug(slugLimpio);
      if (!vigente) return;
      const estado: EstadoSlug = !respuesta
        ? { tipo: "desconocido" }
        : respuesta.disponible
          ? { tipo: "disponible" }
          : { tipo: "ocupado", motivo: respuesta.motivo ?? "Ese link no está disponible." };
      setConsultaSlug({ slug: slugLimpio, estado });
    }, 400);
    return () => {
      vigente = false;
      clearTimeout(espera);
    };
  }, [slugLimpio, esSlugPropio]);

  function enviar(e: React.FormEvent) {
    e.preventDefault();
    const cuerpo = { ...datos, slug: slugLimpio };
    iniciarEnvio(async () => {
      const r = await guardarPerfil(modo, cuerpo, inicial?.slug);
      // Si salió bien, guardarPerfil redirige al panel y no llega acá.
      setResultado(r);
      requestAnimationFrame(() => resumen.current?.focus());
    });
  }

  const errorSlug = errores.slug ?? (estadoSlug.tipo === "ocupado" ? estadoSlug.motivo : undefined);
  const cambiaElSlug = modo === "edicion" && !esSlugPropio;

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-5">
      {resultado && (
        <div
          ref={resumen}
          tabIndex={-1}
          role="alert"
          className="rounded-2xl bg-celeste p-4 text-sm text-confianza-profundo outline-none"
        >
          <p className="font-semibold">{resultado.mensaje}</p>
          {Object.keys(errores).length > 0 && <p>Revisá los campos marcados.</p>}
        </div>
      )}

      <fieldset className="flex flex-col gap-2" aria-describedby={errores.tipo ? "tipo-error" : undefined}>
        <legend className="mb-1.5 text-sm font-semibold">¿Cómo vendés?</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {TIPOS.map((t) => (
            <label
              key={t.valor}
              className="flex min-h-11 cursor-pointer items-start gap-3 rounded-2xl border border-borde-fuerte bg-superficie p-4 has-[:checked]:border-tinta has-[:checked]:ring-1 has-[:checked]:ring-tinta"
            >
              <input
                type="radio"
                name="tipo"
                value={t.valor}
                checked={datos.tipo === t.valor}
                onChange={() => cambiar("tipo", t.valor)}
                className="mt-1 size-4 accent-tinta"
              />
              <span className="flex flex-col">
                <span className="font-semibold">{t.titulo}</span>
                <span className="text-sm text-secundario">{t.detalle}</span>
              </span>
            </label>
          ))}
        </div>
        {errores.tipo && (
          <p id="tipo-error" className="text-sm font-semibold text-marca-oscuro">
            {errores.tipo}
          </p>
        )}
      </fieldset>

      <Campo id="nombrePublico" etiqueta="Nombre público" ayuda="Es el que ven los compradores." error={errores.nombrePublico}>
        <input
          id="nombrePublico"
          type="text"
          maxLength={80}
          autoComplete="organization"
          value={datos.nombrePublico}
          onChange={(e) => cambiarNombre(e.target.value)}
          aria-invalid={!!errores.nombrePublico || undefined}
          aria-describedby={describir("nombrePublico", true, errores.nombrePublico)}
          className={claseEntrada}
        />
      </Campo>

      <Campo
        id="slug"
        etiqueta="Tu link"
        ayuda={
          cambiaElSlug
            ? "Si cambiás el link, el anterior deja de funcionar. Actualizalo donde lo hayas compartido."
            : <EstadoDelSlug estado={estadoSlug} slug={slugLimpio} />
        }
        error={errorSlug}
      >
        <div className="flex min-h-12 items-center rounded-2xl border border-borde-fuerte bg-superficie pl-4 has-aria-invalid:border-marca-oscuro">
          <span className="shrink-0 text-secundario">{urlVisibleDelPerfil("").replace(/\/$/, "")}/</span>
          <input
            id="slug"
            type="text"
            maxLength={40}
            autoCapitalize="none"
            spellCheck={false}
            value={datos.slug}
            onChange={(e) => {
              setSlugEditado(true);
              cambiar("slug", limpiarSlugMientrasSeEscribe(e.target.value));
            }}
            aria-invalid={!!errorSlug || undefined}
            aria-describedby={describir("slug", true, errorSlug)}
            className="min-h-12 w-full min-w-0 rounded-r-2xl bg-transparent pr-4 text-base outline-none"
          />
        </div>
      </Campo>

      <Campo
        id="whatsapp"
        etiqueta="WhatsApp"
        ayuda="Código de área y número, sin 0 ni 15. Por ejemplo: 3534123456. No se muestra en tu perfil."
        error={errores.whatsapp}
      >
        <input
          id="whatsapp"
          type="tel"
          inputMode="tel"
          autoComplete="tel-national"
          value={datos.whatsapp}
          onChange={(e) => cambiar("whatsapp", e.target.value)}
          aria-invalid={!!errores.whatsapp || undefined}
          aria-describedby={describir("whatsapp", true, errores.whatsapp)}
          className={claseEntrada}
        />
      </Campo>

      <Campo
        id="localidad"
        etiqueta="Localidad"
        ayuda="Los compradores te encuentran cuando buscan autos en tu zona."
        error={errores.localidad}
      >
        <BuscadorLocalidad
          id="localidad"
          valor={datos.localidad}
          onCambio={(l) => cambiar("localidad", l)}
          invalido={!!errores.localidad}
          describidoPor={describir("localidad", true, errores.localidad)}
        />
      </Campo>

      <Campo id="direccion" etiqueta="Dirección" opcional error={errores.direccion}>
        <input
          id="direccion"
          type="text"
          maxLength={120}
          autoComplete="street-address"
          placeholder="Bv. España 123"
          value={datos.direccion}
          onChange={(e) => cambiar("direccion", e.target.value)}
          aria-invalid={!!errores.direccion || undefined}
          aria-describedby={describir("direccion", undefined, errores.direccion)}
          className={claseEntrada}
        />
      </Campo>

      <Campo id="horarios" etiqueta="Horarios de atención" opcional error={errores.horarios}>
        <input
          id="horarios"
          type="text"
          maxLength={200}
          placeholder="Lunes a viernes de 9 a 18, sábados de 9 a 13"
          value={datos.horarios}
          onChange={(e) => cambiar("horarios", e.target.value)}
          aria-invalid={!!errores.horarios || undefined}
          aria-describedby={describir("horarios", undefined, errores.horarios)}
          className={claseEntrada}
        />
      </Campo>

      <Campo id="descripcion" etiqueta="Descripción" opcional error={errores.descripcion}>
        <textarea
          id="descripcion"
          rows={4}
          maxLength={1000}
          placeholder="Contá qué vendés, si financiás, si tomás usados…"
          value={datos.descripcion}
          onChange={(e) => cambiar("descripcion", e.target.value)}
          aria-invalid={!!errores.descripcion || undefined}
          aria-describedby={describir("descripcion", undefined, errores.descripcion)}
          className={`${claseEntrada} py-3`}
        />
      </Campo>

      <div className="grid gap-5 sm:grid-cols-2">
        <Campo id="instagram" etiqueta="Instagram" opcional error={errores.instagram}>
          <input
            id="instagram"
            type="text"
            autoCapitalize="none"
            spellCheck={false}
            placeholder="@tuconcesionaria"
            value={datos.instagram}
            onChange={(e) => cambiar("instagram", e.target.value)}
            aria-invalid={!!errores.instagram || undefined}
            aria-describedby={describir("instagram", undefined, errores.instagram)}
            className={claseEntrada}
          />
        </Campo>
        <Campo id="facebook" etiqueta="Facebook" opcional error={errores.facebook}>
          <input
            id="facebook"
            type="text"
            autoCapitalize="none"
            spellCheck={false}
            placeholder="Usuario o link de tu página"
            value={datos.facebook}
            onChange={(e) => cambiar("facebook", e.target.value)}
            aria-invalid={!!errores.facebook || undefined}
            aria-describedby={describir("facebook", undefined, errores.facebook)}
            className={claseEntrada}
          />
        </Campo>
      </div>

      <Campo id="telefono" etiqueta="Teléfono fijo" opcional error={errores.telefono}>
        <input
          id="telefono"
          type="tel"
          maxLength={30}
          autoComplete="tel"
          value={datos.telefono}
          onChange={(e) => cambiar("telefono", e.target.value)}
          aria-invalid={!!errores.telefono || undefined}
          aria-describedby={describir("telefono", undefined, errores.telefono)}
          className={claseEntrada}
        />
      </Campo>

      <Boton type="submit" variante="marca" tamano="lg" disabled={enviando} className="w-full sm:w-auto sm:self-start">
        {enviando ? "Guardando…" : modo === "alta" ? "Crear mi perfil" : "Guardar cambios"}
      </Boton>
    </form>
  );
}

function EstadoDelSlug({ estado, slug }: { estado: EstadoSlug; slug: string }) {
  switch (estado.tipo) {
    case "consultando":
      return <span aria-live="polite">Revisando si está libre…</span>;
    case "disponible":
      return (
        <span aria-live="polite" className="font-semibold text-confianza">
          ✓ {urlVisibleDelPerfil(slug)} está disponible
        </span>
      );
    case "desconocido":
      return <span aria-live="polite">No pudimos revisar si está libre. Lo vamos a validar al guardar.</span>;
    default:
      return <span>Es el link que vas a compartir en Instagram y WhatsApp.</span>;
  }
}
