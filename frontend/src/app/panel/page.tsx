import type { Metadata } from "next";
import { exigirUsuario } from "@/lib/auth/sesion";

export const metadata: Metadata = {
  title: "Panel del vendedor",
  robots: { index: false },
};

export default async function Panel() {
  await exigirUsuario("/panel");

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-3 px-4 pt-6 pb-16">
      <h1 className="font-titulo text-[32px] leading-none font-extrabold tracking-[-1px]">Panel del vendedor</h1>
      <p className="max-w-prose text-secundario">
        Muy pronto vas a poder armar tu perfil y publicar tus autos desde acá.
      </p>
    </main>
  );
}
