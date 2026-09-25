import type { EstadoPublicacion } from "@/lib/api/tipos";
import { ESTADOS } from "@/lib/publicaciones/etiquetas";

// Colores del panel del mockup.
const COLORES: Record<EstadoPublicacion, string> = {
  BORRADOR: "bg-celeste text-confianza-profundo",
  ACTIVA: "bg-[#e3f1e8] text-whatsapp-oscuro",
  PAUSADA: "bg-[#f1ece2] text-secundario",
  VENDIDA: "bg-[#fbe9dd] text-marca-oscuro",
};

export function EstadoBadge({ estado }: { estado: EstadoPublicacion }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${COLORES[estado]}`}>{ESTADOS[estado]}</span>
  );
}
