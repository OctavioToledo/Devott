import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AccionesPublicacion } from "@/components/publicaciones/AccionesPublicacion";
import { EstadoBadge } from "@/components/publicaciones/EstadoBadge";
import { FormularioPublicacion } from "@/components/publicaciones/FormularioPublicacion";
import { obtenerMarcas, obtenerModelos } from "@/lib/api/catalogo";
import { exigirUsuario } from "@/lib/auth/sesion";
import { miPublicacion } from "@/lib/publicaciones/consultas";
import { miVendedor } from "@/lib/vendedores/consultas";

export const metadata: Metadata = {
  title: "Editar publicación",
  robots: { index: false },
};

export default async function EditarPublicacion({ params, searchParams }: PageProps<"/panel/publicaciones/[id]">) {
  const { id } = await params;
  const { nueva } = await searchParams;
  await exigirUsuario(`/panel/publicaciones/${id}`);

  const [publicacion, vendedor, marcas] = await Promise.all([miPublicacion(id), miVendedor(), obtenerMarcas()]);
  if (!publicacion || !vendedor) notFound();
  const modelos = await obtenerModelos(publicacion.modelo.marcaId);

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      <div className="flex flex-col gap-2">
        <Link href="/panel" className="text-sm font-semibold text-secundario">
          ← Volver al panel
        </Link>
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">{publicacion.titulo}</h1>
          <EstadoBadge estado={publicacion.estado} />
        </div>
      </div>

      {nueva && (
        <p role="status" className="rounded-2xl bg-celeste p-4 text-sm font-semibold text-confianza-profundo">
          Guardamos el borrador. Sumá al menos una foto y publicala.
        </p>
      )}

      <section aria-label="Estado de la publicación" className="rounded-[18px] border border-borde bg-superficie p-4">
        <AccionesPublicacion id={publicacion.id} titulo={publicacion.titulo} estado={publicacion.estado} enEdicion />
      </section>

      <FormularioPublicacion
        marcas={marcas}
        inicial={publicacion}
        modelosIniciales={modelos}
        ubicacionDelPerfil={`${vendedor.localidad.ciudad}, ${vendedor.localidad.provincia}`}
      />
    </main>
  );
}
