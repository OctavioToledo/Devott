import type { Metadata } from "next";
import Image from "next/image";
import { Boton } from "@/components/ui/Boton";
import { cerrarSesion } from "@/lib/auth/acciones";
import { exigirUsuario } from "@/lib/auth/sesion";
import { ErrorApi } from "@/lib/api/cliente";
import { pedirApiServidor } from "@/lib/api/servidor";
import type { Me } from "@/lib/api/tipos";

export const metadata: Metadata = {
  title: "Mi cuenta",
  robots: { index: false },
};

const fecha = new Intl.DateTimeFormat("es-AR", { month: "long", year: "numeric" });

export default async function Cuenta() {
  await exigirUsuario("/cuenta");

  let me: Me | null = null;
  let error: string | null = null;
  try {
    me = await pedirApiServidor<Me>("/me");
  } catch (e) {
    if (!(e instanceof ErrorApi)) throw e;
    error = e.detalle;
  }

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      <h1 className="font-titulo text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px]">Mi cuenta</h1>

      {me ? (
        <section className="flex items-center gap-4 rounded-[22px] border border-borde bg-superficie p-5">
          <div className="flex size-14 shrink-0 items-center justify-center overflow-hidden rounded-full bg-fondo text-xl font-semibold">
            {me.avatarUrl ? (
              <Image src={me.avatarUrl} alt="" width={56} height={56} unoptimized />
            ) : (
              <span aria-hidden="true">{(me.nombre ?? me.email ?? "?").charAt(0).toUpperCase()}</span>
            )}
          </div>
          <div className="min-w-0">
            <p className="truncate font-semibold">{me.nombre ?? "Sin nombre"}</p>
            {me.email && <p className="truncate text-sm text-secundario">{me.email}</p>}
            <p className="text-sm text-secundario">En Devott desde {fecha.format(new Date(me.creadoEn))}</p>
          </div>
        </section>
      ) : (
        <p role="alert" className="rounded-2xl bg-celeste p-4 text-sm text-confianza-profundo">
          No pudimos cargar tus datos. {error}
        </p>
      )}

      <form action={cerrarSesion}>
        <Boton type="submit" variante="contorno" tamano="lg" className="w-full">
          Cerrar sesión
        </Boton>
      </form>
    </main>
  );
}
