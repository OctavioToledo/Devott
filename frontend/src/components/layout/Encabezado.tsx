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
        {usuario && (
          <Link
            href="/guardados"
            aria-label="Guardados"
            className="flex size-11 items-center justify-center rounded-full border border-borde-fuerte bg-superficie hover:border-tinta"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 20s-7.5-4.6-7.5-10A4.2 4.2 0 0 1 12 7.6 4.2 4.2 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z" />
            </svg>
          </Link>
        )}
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
