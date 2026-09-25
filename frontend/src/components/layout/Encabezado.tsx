import { BotonLink } from "@/components/ui/Boton";
import { Logo } from "@/components/ui/Logo";

export function Encabezado() {
  return (
    <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 pt-[18px] pb-2">
      <Logo />
      <nav aria-label="Principal" className="flex items-center gap-2">
        <BotonLink href="/panel" variante="contorno">
          Publicá tu auto
        </BotonLink>
      </nav>
    </header>
  );
}
