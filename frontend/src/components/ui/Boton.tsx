import Link, { type LinkProps } from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { clasesBoton, type TamanoBoton, type VarianteBoton } from "./boton";

type Estilo = { variante?: VarianteBoton; tamano?: TamanoBoton; className?: string };

export function Boton({
  variante,
  tamano,
  className,
  type = "button",
  ...props
}: Estilo & ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button type={type} className={clasesBoton(variante, tamano, className)} {...props} />;
}

export function BotonLink({
  variante,
  tamano,
  className,
  children,
  ...props
}: Estilo & LinkProps & { children: ReactNode }) {
  return (
    <Link className={clasesBoton(variante, tamano, className)} {...props}>
      {children}
    </Link>
  );
}
