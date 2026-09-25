import { BotonLink } from "@/components/ui/Boton";
import { Logo } from "@/components/ui/Logo";

export default function NoEncontrado() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-5 px-4 pt-[18px] pb-16">
      <Logo />
      <h1 className="font-titulo mt-6 text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px]">
        No encontramos esta página.
      </h1>
      <p className="text-secundario">Puede que el link esté mal escrito o que la página ya no exista.</p>
      <BotonLink href="/" variante="oscuro" tamano="lg" className="self-start">
        Ir al inicio
      </BotonLink>
    </main>
  );
}
