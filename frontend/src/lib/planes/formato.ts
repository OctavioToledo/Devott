import type { Plan } from "@/lib/api/tipos";

const pesos = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 });
const fechaLarga = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "long", timeZone: "UTC" });

/** "$ 35.000". */
export function precioMensual(plan: Plan): string {
  return `$ ${pesos.format(plan.precioArs)}`;
}

/** "10 de octubre" a partir de "2026-10-10". */
export function fechaDelPlan(iso: string): string {
  return fechaLarga.format(new Date(`${iso}T00:00:00Z`));
}

/** Link de WhatsApp para pedir un plan, con el perfil del vendedor si ya lo tiene. */
export function linkParaPedirPlan(numero: string, plan: Plan, urlPerfil?: string): string {
  const texto =
    `¡Hola! Quiero contratar el plan ${plan.nombre} de Devott (${precioMensual(plan)} por mes).` +
    (urlPerfil ? ` Mi perfil: ${urlPerfil}` : "");
  return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
}
