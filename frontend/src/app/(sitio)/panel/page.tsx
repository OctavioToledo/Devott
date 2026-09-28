import type { Metadata } from "next";
import { BotonLink } from "@/components/ui/Boton";
import { BotonCopiar } from "@/components/ui/BotonCopiar";
import { PanelMetricas } from "@/components/metricas/PanelMetricas";
import { TarjetaPlan } from "@/components/planes/TarjetaPlan";
import { ListaMisPublicaciones } from "@/components/publicaciones/ListaMisPublicaciones";
import { FormularioPerfil } from "@/components/vendedores/FormularioPerfil";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { Metricas, MiPlan } from "@/lib/api/tipos";
import { exigirUsuario } from "@/lib/auth/sesion";
import { leerPeriodo } from "@/lib/metricas/formato";
import { urlDelPerfil, urlVisibleDelPerfil } from "@/lib/vendedores/formato";
import { misPublicaciones } from "@/lib/publicaciones/consultas";
import { miVendedor } from "@/lib/vendedores/consultas";

export const metadata: Metadata = {
  title: "Panel del vendedor",
  robots: { index: false },
};

export default async function Panel({ searchParams }: PageProps<"/panel">) {
  await exigirUsuario("/panel");
  const [vendedor, { guardado, publicacion, dias }] = await Promise.all([miVendedor(), searchParams]);

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

  const periodo = leerPeriodo(dias);
  const [publicaciones, plan, metricas] = await Promise.all([
    misPublicaciones(),
    pedirApiServidor<MiPlan>("/me/suscripcion").catch((e) => {
      if (e instanceof ErrorApi) return null;
      throw e;
    }),
    pedirApiServidor<Metricas>(`/me/metricas?dias=${periodo}`).catch((e) => {
      if (e instanceof ErrorApi) return null;
      throw e;
    }),
  ]);
  const porPublicacion = Object.fromEntries((metricas?.publicaciones ?? []).map((m) => [m.id, m]));

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      {publicacion === "guardada" && (
        <p role="status" className="rounded-2xl bg-celeste p-4 text-sm font-semibold text-confianza-profundo">
          Guardamos los cambios de la publicación.
        </p>
      )}
      {guardado && (
        <p role="status" className="rounded-2xl bg-celeste p-4 text-sm font-semibold text-confianza-profundo">
          {guardado === "alta" ? "¡Listo! Ya creaste tu perfil." : "Guardamos los cambios de tu perfil."}
        </p>
      )}

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-secundario">Hola,</p>
          <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">{vendedor.nombrePublico}</h1>
        </div>
        <BotonLink href="/panel/publicaciones/nueva" variante="marca" tamano="lg">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Publicar vehículo
        </BotonLink>
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

      {plan && <TarjetaPlan plan={plan} />}

      {metricas ? (
        <PanelMetricas metricas={metricas} periodo={periodo} />
      ) : (
        <p role="alert" className="rounded-2xl bg-celeste p-4 text-sm text-confianza-profundo">
          No pudimos cargar tus métricas. Probá de nuevo en un rato.
        </p>
      )}

      <section aria-labelledby="mis-publicaciones" className="flex flex-col gap-3">
        <h2 id="mis-publicaciones" className="font-titulo text-2xl font-extrabold tracking-[-0.6px]">
          Mis publicaciones
        </h2>
        {publicaciones.length > 0 ? (
          <ListaMisPublicaciones publicaciones={publicaciones} metricas={porPublicacion} dias={periodo} />
        ) : (
          <p className="rounded-[18px] border border-dashed border-borde-fuerte p-5 text-sm text-secundario">
            Todavía no publicaste ningún vehículo. Empezá con “Publicar vehículo”.
          </p>
        )}
      </section>
    </main>
  );
}
