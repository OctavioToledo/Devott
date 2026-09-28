import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Galeria } from "@/components/publicaciones/Galeria";
import { Odometro } from "@/components/publicaciones/Odometro";
import type { PublicacionPublica, VendedorPublico } from "@/lib/api/tipos";
import { urlDelSitio } from "@/lib/config";
import { publicacionPublica } from "@/lib/publicaciones/consultas";
import {
  CARROCERIAS,
  COMBUSTIBLES,
  CONDICIONES,
  formatoKm,
  formatoPrecio,
  TRACCIONES,
  TRANSMISIONES,
} from "@/lib/publicaciones/etiquetas";
import { vendedorPublico } from "@/lib/vendedores/consultas";
import { iniciales } from "@/lib/vendedores/formato";

function lugarDe(p: PublicacionPublica): string {
  return [p.ciudad, p.provincia].filter(Boolean).join(", ");
}

/** "Toyota Hilux 2021". */
function nombreCorto(p: PublicacionPublica): string {
  return `${p.titulo} ${p.anio}`;
}

export async function generateMetadata({ params }: PageProps<"/publicaciones/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const p = await publicacionPublica(slug);
  if (!p) return { title: "Publicación no encontrada" };

  const titulo = `${nombreCorto(p)} · ${formatoPrecio(p.precio, p.moneda)}`;
  const descripcion = [
    p.version,
    p.condicion === "0KM" ? "0 km" : `${formatoKm(p.km)} km`,
    lugarDe(p),
    p.vendedor.nombrePublico,
  ]
    .filter(Boolean)
    .join(" · ");
  const portada = p.fotos[0];
  return {
    title: p.estado === "VENDIDA" ? `${titulo} (vendido)` : titulo,
    description: descripcion,
    alternates: { canonical: `/publicaciones/${p.slug}` },
    openGraph: {
      type: "website",
      title: titulo,
      description: descripcion,
      url: `/publicaciones/${p.slug}`,
      images: portada
        ? [{ url: portada.url, width: portada.ancho ?? undefined, height: portada.alto ?? undefined, alt: nombreCorto(p) }]
        : undefined,
    },
    twitter: { card: portada ? "summary_large_image" : "summary" },
  };
}

export default async function DetallePublicacion({ params }: PageProps<"/publicaciones/[slug]">) {
  const { slug } = await params;
  const p = await publicacionPublica(slug);
  if (!p) notFound();
  const vendedor = await vendedorPublico(p.vendedor.slug);

  const lugar = lugarDe(p);
  const vendida = p.estado === "VENDIDA";
  const ficha: [string, string | null][] = [
    ["Año", String(p.anio)],
    ["Kilómetros", `${formatoKm(p.km)} km`],
    ["Condición", CONDICIONES[p.condicion]],
    ["Carrocería", p.carroceria && CARROCERIAS[p.carroceria]],
    ["Combustible", p.combustible && COMBUSTIBLES[p.combustible]],
    ["Transmisión", p.transmision && TRANSMISIONES[p.transmision]],
    ["Tracción", p.traccion && TRACCIONES[p.traccion]],
    ["Color", p.color],
    ["Puertas", p.puertas ? String(p.puertas) : null],
  ];
  const etiquetas = [
    p.financia && "Financia",
    p.aceptaPermuta && "Acepta permuta",
    p.unicoDueno && "Único dueño",
  ].filter((x): x is string => Boolean(x));

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col pb-28 lg:pb-16">
      <script
        type="application/ld+json"
        // Datos estructurados para buscadores (schema.org/Car). Solo texto de la API, escapado por JSON.
        dangerouslySetInnerHTML={{ __html: jsonLd(p).replace(/</g, "\\u003c") }}
      />
      <div className="px-4 pt-2 pb-3">
        <Link href="/" className="inline-flex min-h-11 items-center gap-1.5 text-sm font-semibold text-secundario no-underline hover:text-tinta">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M15 5l-7 7 7 7" />
          </svg>
          Ver más autos
        </Link>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_380px] lg:items-start lg:px-4">
        <div className="flex min-w-0 flex-col gap-6">
          <Galeria fotos={p.fotos} titulo={nombreCorto(p)} />

          <div className="flex flex-col gap-6 px-4 lg:px-0">
            <section aria-labelledby="titulo-ficha" className="flex flex-col gap-3">
              <h2 id="titulo-ficha" className="font-titulo text-[22px] font-extrabold tracking-[-0.5px]">
                Ficha técnica
              </h2>
              <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-[18px] border border-borde bg-borde sm:grid-cols-3">
                {ficha
                  .filter((f): f is [string, string] => Boolean(f[1]))
                  .map(([dato, valor]) => (
                    <div key={dato} className="flex flex-col gap-0.5 bg-superficie px-4 py-3">
                      <dt className="text-xs font-semibold tracking-[0.4px] text-secundario uppercase">{dato}</dt>
                      <dd className="font-semibold">{valor}</dd>
                    </div>
                  ))}
              </dl>
            </section>

            {p.descripcion && (
              <section aria-labelledby="titulo-descripcion" className="flex flex-col gap-2">
                <h2 id="titulo-descripcion" className="font-titulo text-[22px] font-extrabold tracking-[-0.5px]">
                  Descripción
                </h2>
                <p className="text-[15px] leading-relaxed whitespace-pre-line text-[#3b362d]">{p.descripcion}</p>
              </section>
            )}
          </div>
        </div>

        <aside className="flex flex-col gap-4 px-4 lg:sticky lg:top-4 lg:px-0">
          <div className="flex flex-col gap-3 rounded-[22px] border border-borde bg-superficie p-5">
            {vendida && (
              <p className="self-start rounded-md bg-tinta px-2.5 py-1 text-xs font-bold tracking-[0.5px] text-fondo">VENDIDO</p>
            )}
            <div>
              <h1 className="font-titulo text-[28px] leading-[1.05] font-extrabold tracking-[-0.8px]">{p.titulo}</h1>
              {p.version && <p className="mt-1 text-[15px] text-secundario">{p.version}</p>}
            </div>
            <p className={`font-titulo text-[34px] leading-none font-extrabold tracking-[-1px] ${vendida ? "text-secundario line-through" : ""}`}>
              {formatoPrecio(p.precio, p.moneda)}
            </p>
            <div className="flex flex-wrap items-center gap-3 text-[15px]">
              <span className="rounded-md bg-fondo px-2 py-1 text-sm font-bold">{p.anio}</span>
              <Odometro km={p.km} />
              <span className="rounded-md bg-fondo px-2 py-1 text-sm font-semibold">{CONDICIONES[p.condicion]}</span>
            </div>
            {lugar && (
              <p className="flex items-center gap-1.5 text-sm text-secundario">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z" />
                  <circle cx="12" cy="9.5" r="2.5" />
                </svg>
                {lugar}
              </p>
            )}
            {etiquetas.length > 0 && (
              <ul className="flex flex-wrap gap-2">
                {etiquetas.map((e) => (
                  <li key={e} className="rounded-full bg-celeste px-3 py-1.5 text-[13px] font-semibold text-confianza-profundo">
                    {e}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <TarjetaVendedor p={p} vendedor={vendedor} />
        </aside>
      </div>
    </main>
  );
}

function TarjetaVendedor({ p, vendedor }: { p: PublicacionPublica; vendedor: VendedorPublico | null }) {
  const v = p.vendedor;
  const lugar = [v.ciudad, v.provincia].filter(Boolean).join(", ");
  return (
    <Link
      href={`/${v.slug}`}
      className="flex items-center gap-3.5 rounded-[22px] border border-borde bg-superficie p-4 no-underline hover:border-borde-fuerte"
    >
      {vendedor?.logoUrl ? (
        <span className="relative size-14 shrink-0 overflow-hidden rounded-2xl border border-borde bg-superficie">
          <Image src={vendedor.logoUrl} alt="" fill sizes="56px" className="object-contain" unoptimized />
        </span>
      ) : (
        <span aria-hidden="true" className="font-titulo flex size-14 shrink-0 items-center justify-center rounded-2xl bg-marca text-xl font-extrabold text-white">
          {iniciales(v.nombrePublico)}
        </span>
      )}
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate font-bold">{v.nombrePublico}</span>
        <span className={`text-[13px] font-semibold ${v.verificado ? "text-confianza" : "text-secundario"}`}>
          {v.tipo === "CONCESIONARIA" ? "Concesionaria" : "Particular"}
          {v.verificado && " verificada"}
          {lugar && <span className="font-normal text-secundario"> · {lugar}</span>}
        </span>
        <span className="mt-0.5 text-[13px] font-semibold text-marca">Ver todo su stock</span>
      </span>
    </Link>
  );
}

function jsonLd(p: PublicacionPublica): string {
  return JSON.stringify({
    "@context": "https://schema.org",
    "@type": "Car",
    name: nombreCorto(p),
    brand: { "@type": "Brand", name: p.modelo.marca },
    model: p.modelo.modelo,
    vehicleModelDate: String(p.anio),
    mileageFromOdometer: { "@type": "QuantitativeValue", value: p.km, unitCode: "KMT" },
    itemCondition: p.condicion === "0KM" ? "https://schema.org/NewCondition" : "https://schema.org/UsedCondition",
    image: p.fotos.map((f) => f.url),
    url: `${urlDelSitio()}/publicaciones/${p.slug}`,
    offers: {
      "@type": "Offer",
      price: p.precio,
      priceCurrency: p.moneda,
      availability: p.estado === "VENDIDA" ? "https://schema.org/SoldOut" : "https://schema.org/InStock",
      seller: { "@type": "Organization", name: p.vendedor.nombrePublico },
    },
  });
}
