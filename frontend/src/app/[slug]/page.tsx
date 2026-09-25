import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { clasesBoton } from "@/components/ui/boton";
import { BotonCopiar } from "@/components/ui/BotonCopiar";
import { TarjetaStock } from "@/components/publicaciones/TarjetaStock";
import { Logo } from "@/components/ui/Logo";
import type { Condicion, VendedorPublico } from "@/lib/api/tipos";
import { stockDeVendedor } from "@/lib/publicaciones/consultas";
import { iniciales, urlDelPerfil, urlVisibleDelPerfil } from "@/lib/vendedores/formato";
import { vendedorPublico } from "@/lib/vendedores/consultas";

const FILTROS: { valor: string | null; texto: string; condicion?: Condicion }[] = [
  { valor: null, texto: "Todos" },
  { valor: "0km", texto: "0 km", condicion: "0KM" },
  { valor: "usados", texto: "Usados", condicion: "USADO" },
];

function ubicacion(v: VendedorPublico): string {
  return [v.direccion, v.ciudad, v.provincia].filter(Boolean).join(", ");
}

export async function generateMetadata({ params }: PageProps<"/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const vendedor = await vendedorPublico(slug);
  if (!vendedor) return { title: "Perfil no encontrado" };

  const lugar = [vendedor.ciudad, vendedor.provincia].filter(Boolean).join(", ");
  const descripcion =
    vendedor.descripcion?.slice(0, 160) ??
    `Autos de ${vendedor.nombrePublico}${lugar ? ` en ${lugar}` : ""}. Mirá el stock y consultá por WhatsApp.`;
  return {
    title: vendedor.nombrePublico,
    description: descripcion,
    alternates: { canonical: `/${vendedor.slug}` },
    openGraph: {
      type: "profile",
      title: `${vendedor.nombrePublico} · Devott`,
      description: descripcion,
      url: `/${vendedor.slug}`,
      images: vendedor.logoUrl ? [{ url: vendedor.logoUrl, alt: `Logo de ${vendedor.nombrePublico}` }] : undefined,
    },
  };
}

export default async function PerfilPublico({ params, searchParams }: PageProps<"/[slug]">) {
  const { slug } = await params;
  const { contacto, condicion } = await searchParams;
  const vendedor = await vendedorPublico(slug);
  if (!vendedor) notFound();

  const filtro = FILTROS.find((f) => f.valor === condicion) ?? FILTROS[0];
  const stock = await stockDeVendedor(vendedor.slug, filtro.condicion);

  const esConcesionaria = vendedor.tipo === "CONCESIONARIA";
  const lugar = ubicacion(vendedor);

  return (
    <main className="flex flex-1 flex-col">
      <div className="bg-confianza-profundo">
        <div className="mx-auto flex h-[150px] w-full max-w-3xl items-start justify-between gap-3 px-4 pt-3.5">
          <Logo tono="claro" className="text-[22px]" />
          <span className="truncate rounded-full border border-[#4f74a3] px-2.5 py-1.5 text-xs text-celeste">
            {urlVisibleDelPerfil(vendedor.slug)}
          </span>
        </div>
      </div>

      <div className="mx-auto -mt-11 flex w-full max-w-3xl flex-col gap-3.5 px-4">
        {vendedor.logoUrl ? (
          <div className="relative size-[88px] overflow-hidden rounded-3xl border-4 border-fondo bg-superficie">
            <Image src={vendedor.logoUrl} alt={`Logo de ${vendedor.nombrePublico}`} fill sizes="88px" className="object-contain" unoptimized />
          </div>
        ) : (
          <div
            aria-hidden="true"
            className="font-titulo flex size-[88px] items-center justify-center rounded-3xl border-4 border-fondo bg-marca text-[32px] font-extrabold text-white"
          >
            {iniciales(vendedor.nombrePublico)}
          </div>
        )}

        <div>
          <h1 className="font-titulo text-[30px] leading-[1.05] font-extrabold tracking-[-1px]">{vendedor.nombrePublico}</h1>
          {vendedor.verificado ? (
            <p className="mt-1.5 flex items-center gap-1.5 text-sm font-semibold text-confianza">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M12 3l2.4 1.8 3-.3 1 2.8 2.5 1.7-1 2.8 1 2.8-2.5 1.7-1 2.8-3-.3L12 21l-2.4-1.8-3 .3-1-2.8-2.5-1.7 1-2.8-1-2.8 2.5-1.7 1-2.8 3 .3z" />
                <path d="M8.5 12l2.5 2.5 4.5-5" fill="none" stroke="#fff" strokeWidth="2" />
              </svg>
              {esConcesionaria ? "Concesionaria verificada" : "Vendedor verificado"}
            </p>
          ) : (
            <p className="mt-1.5 text-xs font-bold tracking-[0.6px] text-secundario uppercase">
              {esConcesionaria ? "Concesionaria" : "Particular"}
            </p>
          )}
        </div>

        <ul className="flex flex-col gap-2 text-[15px] text-[#3b362d]">
          {lugar && (
            <DatoConIcono icono={<IconoUbicacion />}>{lugar}</DatoConIcono>
          )}
          {vendedor.horarios && <DatoConIcono icono={<IconoReloj />}>{vendedor.horarios}</DatoConIcono>}
          {vendedor.instagram && (
            <DatoConIcono icono={<IconoInstagram />}>
              <a href={`https://instagram.com/${vendedor.instagram}`} rel="noopener noreferrer" target="_blank" className="underline-offset-2 hover:underline">
                @{vendedor.instagram}
              </a>
            </DatoConIcono>
          )}
          {vendedor.facebook && (
            <DatoConIcono icono={<IconoFacebook />}>
              <a href={`https://facebook.com/${vendedor.facebook}`} rel="noopener noreferrer" target="_blank" className="underline-offset-2 hover:underline">
                facebook.com/{vendedor.facebook}
              </a>
            </DatoConIcono>
          )}
        </ul>

        {vendedor.descripcion && <p className="whitespace-pre-line text-[15px] text-secundario">{vendedor.descripcion}</p>}

        {contacto === "error" && (
          <p role="alert" className="rounded-2xl bg-celeste p-3 text-sm text-confianza-profundo">
            No pudimos abrir WhatsApp. Probá de nuevo en un rato.
          </p>
        )}

        <div className="flex gap-2">
          <a
            href={`/contacto/vendedor/${vendedor.slug}`}
            rel="nofollow"
            className={clasesBoton("whatsapp", "lg", "flex-1 font-bold")}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M4 20l1.3-3.9A8 8 0 1 1 8 19.2L4 20z" />
            </svg>
            WhatsApp
          </a>
          <BotonCopiar texto={urlDelPerfil(vendedor.slug)} tamano="lg" className="flex-1 font-bold" />
        </div>
      </div>

      <section id="stock" aria-labelledby="titulo-stock" className="mx-auto flex w-full max-w-3xl flex-col gap-3.5 px-4 pt-7">
        <div className="flex items-baseline justify-between gap-3">
          <h2 id="titulo-stock" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
            Stock disponible
          </h2>
          <span className="text-sm text-secundario">
            {stock.total === 1 ? "1 unidad" : `${stock.total} unidades`}
          </span>
        </div>
        <nav aria-label="Filtrar stock" className="flex gap-2">
          {FILTROS.map((f) => {
            const activo = f === filtro;
            return (
              <Link
                key={f.texto}
                href={f.valor ? `/${vendedor.slug}?condicion=${f.valor}#stock` : `/${vendedor.slug}#stock`}
                aria-current={activo ? "page" : undefined}
                scroll={false}
                className={`flex min-h-11 items-center rounded-full border-[1.5px] px-4 text-sm font-semibold no-underline ${
                  activo ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie text-tinta"
                }`}
              >
                {f.texto}
              </Link>
            );
          })}
        </nav>
        {stock.items.length > 0 ? (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {stock.items.map((p) => (
              <li key={p.slug}>
                <TarjetaStock publicacion={p} />
              </li>
            ))}
          </ul>
        ) : (
          <p className="rounded-[18px] border border-dashed border-borde-fuerte p-5 text-sm text-secundario">
            {filtro.condicion ? "No hay unidades con este filtro." : "Todavía no hay autos publicados. Volvé a pasar pronto."}
          </p>
        )}
      </section>

      <footer className="flex justify-center px-4 py-7">
        <Link href="/panel" className="flex items-center gap-1.5 text-[13px] text-secundario no-underline">
          Catálogo creado con{" "}
          <span className="font-titulo font-extrabold text-tinta">
            devott<span className="text-marca">.</span>
          </span>{" "}
          · Publicá el tuyo
        </Link>
      </footer>
    </main>
  );
}

function DatoConIcono({ icono, children }: { icono: React.ReactNode; children: React.ReactNode }) {
  return (
    <li className="flex items-start gap-2">
      <span className="mt-0.5 shrink-0 text-secundario">{icono}</span>
      <span className="min-w-0 break-words">{children}</span>
    </li>
  );
}

const propsIcono = {
  width: 18,
  height: 18,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
};

function IconoUbicacion() {
  return (
    <svg {...propsIcono}>
      <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z" />
      <circle cx="12" cy="9.5" r="2.5" />
    </svg>
  );
}

function IconoReloj() {
  return (
    <svg {...propsIcono}>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v4l3 2" />
    </svg>
  );
}

function IconoInstagram() {
  return (
    <svg {...propsIcono}>
      <rect x="4" y="4" width="16" height="16" rx="5" />
      <circle cx="12" cy="12" r="3.5" />
    </svg>
  );
}

function IconoFacebook() {
  return (
    <svg {...propsIcono}>
      <path d="M15 8h-2a2 2 0 0 0-2 2v11M8 13h6" />
    </svg>
  );
}
