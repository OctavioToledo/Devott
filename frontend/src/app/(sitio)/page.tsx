import type { Metadata } from "next";
import Link from "next/link";
import { ListaFeed } from "@/components/feed/ListaFeed";
import { SelectorOrden } from "@/components/feed/SelectorOrden";
import { SelectorZona } from "@/components/feed/SelectorZona";
import { clasesBoton } from "@/components/ui/boton";
import { ErrorApi } from "@/lib/api/cliente";
import type { Condicion, Pagina, TarjetaPublicacion } from "@/lib/api/tipos";
import {
  cantidadDeFiltros,
  FILTROS_VACIOS,
  leerFiltros,
  RADIOS_KM,
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

  let resultado: Pagina<TarjetaPublicacion> | null = null;
  let error: string | null = null;
  try {
    resultado = await buscarPublicaciones(filtros);
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    error = e.status === 0 || e.status >= 500 ? "No pudimos cargar los autos. Probá de nuevo en un rato." : e.detalle;
  }

  const url = urlDelFeed(filtros);
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 px-4 pt-4 pb-16">
      <h1 className="font-titulo max-w-[16ch] text-[32px] leading-[1.02] font-extrabold tracking-[-1.2px] sm:text-5xl">
        Encontrá tu próximo auto cerca de casa.
      </h1>

      <div className="flex flex-col gap-3 lg:flex-row lg:items-start">
        <div className="lg:w-[420px]">
          <SelectorZona key={url} filtros={filtros} />
        </div>
        <nav aria-label="Condición" className="flex gap-2 overflow-x-auto lg:ml-auto">
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
    </main>
  );
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
          <Link href={urlDelFeed({ ...FILTROS_VACIOS, zona, orden: filtros.orden })} className={clasesBoton("suave")}>
            Quitar filtros
          </Link>
        )}
      </div>
    </div>
  );
}
