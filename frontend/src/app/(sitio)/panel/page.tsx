import type { Metadata } from "next";
import { BotonLink } from "@/components/ui/Boton";
import { BotonCopiar } from "@/components/ui/BotonCopiar";
import { FormularioPerfil } from "@/components/vendedores/FormularioPerfil";
import { exigirUsuario } from "@/lib/auth/sesion";
import { urlDelPerfil, urlVisibleDelPerfil } from "@/lib/vendedores/formato";
import { miVendedor } from "@/lib/vendedores/consultas";

export const metadata: Metadata = {
  title: "Panel del vendedor",
  robots: { index: false },
};

export default async function Panel({ searchParams }: PageProps<"/panel">) {
  await exigirUsuario("/panel");
  const [vendedor, { guardado }] = await Promise.all([miVendedor(), searchParams]);

  if (!vendedor) {
    return (
      <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
        <div className="flex flex-col gap-2">
          <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">Creá tu perfil de vendedor</h1>
          <p className="text-secundario">
            Con tu perfil tenés un link propio para compartir tu stock en Instagram y WhatsApp.
          </p>
        </div>
        <FormularioPerfil modo="alta" />
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      {guardado && (
        <p role="status" className="rounded-2xl bg-celeste p-4 text-sm font-semibold text-confianza-profundo">
          {guardado === "alta" ? "¡Listo! Ya creaste tu perfil." : "Guardamos los cambios de tu perfil."}
        </p>
      )}

      <div>
        <p className="text-sm text-secundario">Hola,</p>
        <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">{vendedor.nombrePublico}</h1>
      </div>

      <section
        aria-labelledby="tu-link"
        className="flex flex-col gap-4 rounded-[18px] border border-borde bg-superficie p-5 sm:flex-row sm:items-center sm:justify-between"
      >
        <div className="min-w-0">
          <h2 id="tu-link" className="text-sm text-secundario">
            Tu link
          </h2>
          <p className="truncate text-lg font-semibold">{urlVisibleDelPerfil(vendedor.slug)}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <BotonCopiar texto={urlDelPerfil(vendedor.slug)} variante="suave" />
          <BotonLink href={`/${vendedor.slug}`} variante="suave">
            Ver mi perfil
          </BotonLink>
          <BotonLink href="/panel/perfil" variante="oscuro">
            Editar perfil
          </BotonLink>
        </div>
      </section>

      <section className="rounded-[18px] border border-dashed border-borde-fuerte p-5">
        <h2 className="font-semibold">Tus publicaciones</h2>
        <p className="text-sm text-secundario">Muy pronto vas a poder publicar tus autos desde acá.</p>
      </section>
    </main>
  );
}
