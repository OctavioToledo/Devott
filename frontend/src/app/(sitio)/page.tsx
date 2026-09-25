export default function Inicio() {
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 px-4 pt-6 pb-16">
      <h1 className="font-titulo max-w-[14ch] text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px] sm:text-5xl">
        Encontrá tu próximo auto cerca de casa.
      </h1>
      <p className="max-w-prose text-secundario">
        Autos 0 km y usados de concesionarias y particulares de tu zona. Muy pronto vas a poder buscar y
        filtrar acá.
      </p>
    </main>
  );
}
