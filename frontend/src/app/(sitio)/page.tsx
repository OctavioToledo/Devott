import type { Metadata } from "next";
import Link from "next/link";
import { ListaFeed } from "@/components/feed/ListaFeed";
import { BarraFiltros, BotonFiltros } from "@/components/feed/PanelFiltros";
import { SelectorOrden } from "@/components/feed/SelectorOrden";
import { SelectorZona } from "@/components/feed/SelectorZona";
import { clasesBoton } from "@/components/ui/boton";
import { obtenerMarcas, obtenerModelos } from "@/lib/api/catalogo";
import { ErrorApi } from "@/lib/api/cliente";
import type { Condicion, Marca, Modelo, Pagina, TarjetaPublicacion } from "@/lib/api/tipos";
import {
  cantidadDeFiltros,
  filtrosActivos,
  leerFiltros,
  RADIOS_KM,
  sinFiltros,
  urlDelFeed,
  type FiltrosFeed,
} from "@/lib/publicaciones/busqueda";
import { buscarPublicaciones } from "@/lib/publicaciones/consultas";

const CONDICIONES: { texto: string; condicion: Condicion | null }[] = [
  { texto: "Todos", condicion: null },
  { texto: "0 km", condicion: "0KM" },
  { texto: "Usados", condicion: "USADO" },
];

export async function generateMetadata({ searchParams }: PageProps<"/">): Promise<Metadata> {
  const filtros = leerFiltros(await searchParams);
  const lugar = filtros.zona && filtros.zona.nombre !== "Tu ubicación" ? filtros.zona.nombre : null;
  return {
    title: lugar ? `Autos en ${lugar}` : { absolute: "Devott · Autos en tu zona" },
    // La home sin filtros es la versión canónica; las combinaciones de filtros no se indexan por separado.
    alternates: { canonical: "/" },
    robots: cantidadDeFiltros(filtros) > 0 ? { index: false, follow: true } : undefined,
  };
}

export default async function Inicio({ searchParams }: PageProps<"/">) {
  const filtros = leerFiltros(await searchParams);
  const { marcas, modelos } = await catalogo(filtros.marca);

  let resultado: Pagina<TarjetaPublicacion> | null = null;
  let error: string | null = null;
  try {
    resultado = await buscarPublicaciones(filtros);
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    error = e.status === 0 || e.status >= 500 ? "No pudimos cargar los autos. Probá de nuevo en un rato." : e.detalle;
  }

  const url = urlDelFeed(filtros);
  const nombreMarca = marcas.find((m) => m.slug === filtros.marca)?.nombre;
  const nombreModelo = modelos.find((m) => m.slug === filtros.modelo)?.nombre;
  const chips = filtrosActivos(filtros, { marca: nombreMarca, modelo: nombreModelo });
  const total = resultado?.total ?? 0;
  const panel = { filtros, total, marcas, modelos };

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 px-4 pt-4 pb-16">
      <h1 className="font-titulo max-w-[16ch] text-[32px] leading-[1.02] font-extrabold tracking-[-1.2px] sm:text-5xl">
        {nombreMarca ? `${nombreMarca}${nombreModelo ? ` ${nombreModelo}` : ""} cerca de casa.` : "Encontrá tu próximo auto cerca de casa."}
      </h1>

      <div className="grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)] lg:items-start">
        <BarraFiltros key={`barra-${url}`} {...panel} />

        <div className="flex min-w-0 flex-col gap-4">
          <SelectorZona key={url} filtros={filtros} />

          <div className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 lg:mx-0 lg:px-0">
            <BotonFiltros key={`boton-${url}`} {...panel} cantidad={chips.length} />
            <nav aria-label="Condición" className="flex gap-2">
              {CONDICIONES.map(({ texto, condicion }) => {
                const activo = filtros.condicion === condicion;
                return (
                  <Link
                    key={texto}
                    href={urlDelFeed({ ...filtros, condicion })}
                    scroll={false}
                    aria-current={activo ? "page" : undefined}
                    className={`flex min-h-11 shrink-0 items-center rounded-full border-[1.5px] px-4 text-sm font-semibold no-underline ${
                      activo ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie text-tinta"
                    }`}
                  >
                    {texto}
                  </Link>
                );
              })}
            </nav>
          </div>

          {chips.length > 0 && (
            <ul aria-label="Filtros aplicados" className="flex flex-wrap gap-2">
              {chips.map((c) => (
                <li key={c.clave}>
                  <Link
                    href={urlDelFeed(c.sin)}
                    scroll={false}
                    aria-label={`Quitar filtro ${c.texto}`}
                    className="flex min-h-11 items-center gap-1.5 rounded-full bg-celeste pr-3 pl-4 text-sm font-semibold text-confianza-profundo no-underline hover:bg-[#c9dff2]"
                  >
                    {c.texto}
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
                      <path d="M7 7l10 10M17 7L7 17" />
                    </svg>
                  </Link>
                </li>
              ))}
              {chips.length > 1 && (
                <li>
                  <Link href={urlDelFeed(sinFiltros(filtros))} scroll={false} className="flex min-h-11 items-center px-2 text-sm font-semibold text-secundario">
                    Limpiar todo
                  </Link>
                </li>
              )}
            </ul>
          )}

          {error ? (
            <p role="alert" className="rounded-2xl bg-celeste p-4 text-sm text-confianza-profundo">
              {error}
            </p>
          ) : resultado ? (
            <section aria-labelledby="titulo-resultados" className="flex flex-col gap-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h2 id="titulo-resultados" className="text-[15px] font-semibold" aria-live="polite">
                  {textoTotal(resultado.total)}
                </h2>
                {resultado.total > 1 && <SelectorOrden filtros={filtros} />}
              </div>
              {resultado.items.length > 0 ? (
                <ListaFeed key={url} filtros={filtros} inicial={resultado} />
              ) : (
                <SinResultados filtros={filtros} />
              )}
            </section>
          ) : null}
        </div>
      </div>
    </main>
  );
}

/** Marcas para el panel y, si hay una elegida, sus modelos. Si el catálogo falla, el feed igual se muestra. */
async function catalogo(marcaSlug: string | null): Promise<{ marcas: Marca[]; modelos: Modelo[] }> {
  try {
    const marcas = await obtenerMarcas();
    const marca = marcas.find((m) => m.slug === marcaSlug);
    return { marcas, modelos: marca ? await obtenerModelos(marca.id) : [] };
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    return { marcas: [], modelos: [] };
  }
}

function textoTotal(total: number): string {
  if (total === 0) return "No encontramos autos";
  return total === 1 ? "1 auto" : `${new Intl.NumberFormat("es-AR").format(total)} autos`;
}

function SinResultados({ filtros }: { filtros: FiltrosFeed }) {
  const zona = filtros.zona;
  const radioMayor = zona ? RADIOS_KM.find((r) => r > zona.radioKm) : undefined;
  const hayFiltros = cantidadDeFiltros(filtros) > 0;
  return (
    <div className="flex flex-col items-start gap-3 rounded-[20px] border border-dashed border-borde-fuerte p-5">
      <p className="text-[15px] text-secundario">
        {zona
          ? `No hay autos publicados con estos filtros a menos de ${zona.radioKm} km de ${zona.nombre}.`
          : hayFiltros
            ? "No hay autos publicados con estos filtros."
            : "Todavía no hay autos publicados. Volvé a pasar pronto."}
      </p>
      <div className="flex flex-wrap gap-2">
        {zona && radioMayor && (
          <Link href={urlDelFeed({ ...filtros, zona: { ...zona, radioKm: radioMayor } })} className={clasesBoton("oscuro")}>
            Buscar a {radioMayor} km
          </Link>
        )}
        {hayFiltros && (
          <Link href={urlDelFeed(sinFiltros(filtros))} className={clasesBoton("suave")}>
            Quitar filtros
          </Link>
        )}
      </div>
    </div>
  );
}
