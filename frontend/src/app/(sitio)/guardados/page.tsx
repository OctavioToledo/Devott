import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { TarjetaFeed } from "@/components/feed/TarjetaFeed";
import { BotonSeguir } from "@/components/interacciones/BotonSeguir";
import { clasesBoton } from "@/components/ui/boton";
import { ErrorApi } from "@/lib/api/cliente";
import type { Pagina, TarjetaPublicacion, VendedorSeguido } from "@/lib/api/tipos";
import { exigirUsuario } from "@/lib/auth/sesion";
import { misGuardados, misSeguimientos } from "@/lib/interacciones/consultas";
import { iniciales } from "@/lib/vendedores/formato";

export const metadata: Metadata = {
  title: "Guardados",
  robots: { index: false },
};

const PESTANAS = [
  { valor: "autos", texto: "Autos" },
  { valor: "vendedores", texto: "Vendedores que seguís" },
] as const;

export default async function Guardados({ searchParams }: PageProps<"/guardados">) {
  const { pestana } = await searchParams;
  const actual = pestana === "vendedores" ? "vendedores" : "autos";
  const aca = actual === "autos" ? "/guardados" : "/guardados?pestana=vendedores";
  await exigirUsuario(aca);

  let autos: Pagina<TarjetaPublicacion> | null = null;
  let vendedores: VendedorSeguido[] | null = null;
  let error: string | null = null;
  try {
    if (actual === "autos") autos = await misGuardados();
    else vendedores = await misSeguimientos();
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    error = e.status === 0 || e.status >= 500 ? "No pudimos cargar tus guardados. Probá de nuevo en un rato." : e.detalle;
  }

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 px-4 pt-6 pb-16">
      <h1 className="font-titulo text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px]">Guardados</h1>

      <nav aria-label="Secciones" className="flex gap-2">
        {PESTANAS.map((p) => {
          const activa = p.valor === actual;
          return (
            <Link
              key={p.valor}
              href={p.valor === "autos" ? "/guardados" : "/guardados?pestana=vendedores"}
              aria-current={activa ? "page" : undefined}
              className={`flex min-h-11 items-center rounded-full border-[1.5px] px-4 text-sm font-semibold no-underline ${
                activa ? "border-tinta bg-tinta text-fondo" : "border-borde-fuerte bg-superficie text-tinta"
              }`}
            >
              {p.texto}
            </Link>
          );
        })}
      </nav>

      {error && (
        <p role="alert" className="rounded-2xl bg-celeste p-4 text-sm text-confianza-profundo">
          {error}
        </p>
      )}

      {autos &&
        (autos.items.length > 0 ? (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {autos.items.map((p) => (
              <li key={p.slug}>
                <TarjetaFeed publicacion={p} guardado logueado volverA={aca} />
              </li>
            ))}
          </ul>
        ) : (
          <Vacio texto="Todavía no guardaste autos. Tocá el corazón en los que te gusten para encontrarlos acá." />
        ))}

      {vendedores &&
        (vendedores.length > 0 ? (
          <ul className="grid gap-3 sm:grid-cols-2">
            {vendedores.map((v) => (
              <li key={v.slug}>
                <VendedorSeguidoTarjeta vendedor={v} volverA={aca} />
              </li>
            ))}
          </ul>
        ) : (
          <Vacio texto="Todavía no seguís a ningún vendedor. Tocá Seguir en su perfil para tenerlo a mano." />
        ))}
    </main>
  );
}

function Vacio({ texto }: { texto: string }) {
  return (
    <div className="flex flex-col items-start gap-3 rounded-[20px] border border-dashed border-borde-fuerte p-5">
      <p className="text-[15px] text-secundario">{texto}</p>
      <Link href="/" className={clasesBoton("oscuro")}>
        Buscar autos
      </Link>
    </div>
  );
}

function VendedorSeguidoTarjeta({ vendedor: v, volverA }: { vendedor: VendedorSeguido; volverA: string }) {
  const lugar = [v.ciudad, v.provincia].filter(Boolean).join(", ");
  return (
    <div className="relative flex items-center gap-3.5 rounded-[22px] border border-borde bg-superficie p-4 hover:border-borde-fuerte">
      {v.logoUrl ? (
        <span className="relative size-14 shrink-0 overflow-hidden rounded-2xl border border-borde bg-superficie">
          <Image src={v.logoUrl} alt="" fill sizes="56px" className="object-contain" unoptimized />
        </span>
      ) : (
        <span aria-hidden="true" className="font-titulo flex size-14 shrink-0 items-center justify-center rounded-2xl bg-marca text-xl font-extrabold text-white">
          {iniciales(v.nombrePublico)}
        </span>
      )}
      <span className="flex min-w-0 flex-1 flex-col">
        <Link href={`/${v.slug}`} className="truncate font-bold no-underline after:absolute after:inset-0 after:content-['']">
          {v.nombrePublico}
        </Link>
        <span className={`text-[13px] font-semibold ${v.verificado ? "text-confianza" : "text-secundario"}`}>
          {v.tipo === "CONCESIONARIA" ? "Concesionaria" : "Particular"}
          {v.verificado && " verificada"}
          {lugar && <span className="font-normal text-secundario"> · {lugar}</span>}
        </span>
        <span className="mt-0.5 text-[13px] text-secundario">
          {v.autosActivos === 1 ? "1 auto publicado" : `${v.autosActivos} autos publicados`}
        </span>
      </span>
      <BotonSeguir vendedorSlug={v.slug} siguiendo logueado volverA={volverA} className="shrink-0" />
    </div>
  );
}
