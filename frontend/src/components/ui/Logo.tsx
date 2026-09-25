import Link from "next/link";

export function Logo({ className = "" }: { className?: string }) {
  return (
    <Link
      href="/"
      aria-label="Devott, ir al inicio"
      className={`font-titulo text-[30px] leading-none font-extrabold tracking-[-1px] text-tinta no-underline ${className}`}
    >
      devott<span className="text-marca">.</span>
    </Link>
  );
}
