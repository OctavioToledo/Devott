import Link from "next/link";

/** Logo "devott." con el punto naranja. `tono="claro"` es para fondos oscuros. */
export function Logo({ className = "", tono = "oscuro" }: { className?: string; tono?: "oscuro" | "claro" }) {
  const colores = tono === "claro" ? "text-fondo" : "text-tinta";
  const punto = tono === "claro" ? "text-marca-claro" : "text-marca";
  return (
    <Link
      href="/"
      aria-label="Devott, ir al inicio"
      className={`font-titulo text-[30px] leading-none font-extrabold tracking-[-1px] no-underline ${colores} ${className}`}
    >
      devott<span className={punto}>.</span>
    </Link>
  );
}
