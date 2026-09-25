import Image from "next/image";
import Link from "next/link";
import { BotonLink } from "@/components/ui/Boton";
import { Logo } from "@/components/ui/Logo";
import { usuarioActual } from "@/lib/auth/sesion";

export async function Encabezado() {
  const usuario = await usuarioActual();

  return (
    <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 pt-[18px] pb-2">
      <Logo />
      <nav aria-label="Principal" className="flex items-center gap-2">
        <BotonLink href="/panel" variante="contorno">
          Publicá tu auto
        </BotonLink>
        {usuario ? (
          <Link
            href="/cuenta"
            aria-label="Mi cuenta"
            className="flex size-11 items-center justify-center overflow-hidden rounded-full border border-borde-fuerte bg-superficie font-semibold"
          >
            {usuario.avatarUrl ? (
              // Avatar del proveedor (Google, etc.): no pasa por el optimizador de imágenes.
              <Image src={usuario.avatarUrl} alt="" width={44} height={44} unoptimized />
            ) : (
              <span aria-hidden="true">{(usuario.nombre ?? usuario.email ?? "?").charAt(0).toUpperCase()}</span>
            )}
          </Link>
        ) : (
          <BotonLink href="/ingresar" variante="suave">
            Ingresar
          </BotonLink>
        )}
      </nav>
    </header>
  );
}
