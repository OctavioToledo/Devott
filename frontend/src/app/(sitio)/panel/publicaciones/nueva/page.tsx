import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { FormularioPublicacion } from "@/components/publicaciones/FormularioPublicacion";
import { obtenerMarcas } from "@/lib/api/catalogo";
import { exigirUsuario } from "@/lib/auth/sesion";
import { miVendedor } from "@/lib/vendedores/consultas";

export const metadata: Metadata = {
  title: "Publicar vehículo",
  robots: { index: false },
};

export default async function NuevaPublicacion() {
  await exigirUsuario("/panel/publicaciones/nueva");
  const [vendedor, marcas] = await Promise.all([miVendedor(), obtenerMarcas()]);
  if (!vendedor) redirect("/panel");

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      <div className="flex flex-col gap-2">
        <Link href="/panel" className="text-sm font-semibold text-secundario">
          ← Volver al panel
        </Link>
        <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">Publicar vehículo</h1>
        <p className="text-secundario">Primero los datos; en el paso siguiente cargás las fotos y la publicás.</p>
      </div>
      <FormularioPublicacion
        marcas={marcas}
        ubicacionDelPerfil={`${vendedor.localidad.ciudad}, ${vendedor.localidad.provincia}`}
      />
    </main>
  );
}
