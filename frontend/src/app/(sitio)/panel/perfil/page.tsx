import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { FormularioPerfil } from "@/components/vendedores/FormularioPerfil";
import { GestorLogo } from "@/components/vendedores/GestorLogo";
import { exigirUsuario } from "@/lib/auth/sesion";
import { miVendedor } from "@/lib/vendedores/consultas";

export const metadata: Metadata = {
  title: "Editar perfil",
  robots: { index: false },
};

export default async function EditarPerfil() {
  await exigirUsuario("/panel/perfil");
  const vendedor = await miVendedor();
  if (!vendedor) redirect("/panel");

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      <div className="flex flex-col gap-2">
        <Link href="/panel" className="text-sm font-semibold text-secundario">
          ← Volver al panel
        </Link>
        <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">Editar perfil</h1>
      </div>
      <GestorLogo nombre={vendedor.nombrePublico} logoInicial={vendedor.logoUrl} />
      <FormularioPerfil modo="edicion" inicial={vendedor} />
    </main>
  );
}
