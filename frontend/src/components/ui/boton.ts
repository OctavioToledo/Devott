export type VarianteBoton = "marca" | "oscuro" | "contorno" | "whatsapp" | "suave";
export type TamanoBoton = "md" | "lg";

const base =
  "inline-flex items-center justify-center gap-2 font-semibold no-underline transition-colors " +
  "disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer";

const variantes: Record<VarianteBoton, string> = {
  marca: "bg-marca text-white hover:bg-marca-oscuro",
  oscuro: "bg-tinta text-fondo hover:bg-black",
  contorno: "border-[1.5px] border-tinta text-tinta hover:bg-tinta hover:text-fondo",
  whatsapp: "bg-whatsapp text-white hover:bg-whatsapp-oscuro",
  suave: "border border-borde-fuerte bg-superficie text-tinta hover:border-tinta",
};

// Todos los tamaños respetan el área táctil mínima de 44 px.
const tamanos: Record<TamanoBoton, string> = {
  md: "min-h-11 rounded-full px-4 text-sm",
  lg: "min-h-[50px] rounded-2xl px-5 text-[15px]",
};

export function clasesBoton(
  variante: VarianteBoton = "oscuro",
  tamano: TamanoBoton = "md",
  extra?: string,
): string {
  return [base, variantes[variante], tamanos[tamano], extra].filter(Boolean).join(" ");
}
