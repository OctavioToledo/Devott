import type { TotalesMetricas } from "@/lib/api/tipos";

export type ClaveMetrica = keyof TotalesMetricas;

export const METRICAS: Record<ClaveMetrica, { titulo: string; singular: string; plural: string }> = {
  vistasPerfil: { titulo: "Visitas al perfil", singular: "visita", plural: "visitas" },
  vistasPublicaciones: { titulo: "Vistas de publicaciones", singular: "vista", plural: "vistas" },
  contactos: { titulo: "Clics a WhatsApp", singular: "clic", plural: "clics" },
  guardados: { titulo: "Guardados", singular: "guardado", plural: "guardados" },
};

export const PERIODOS = [7, 30, 90] as const;
export type Periodo = (typeof PERIODOS)[number];

export function leerPeriodo(valor: string | string[] | undefined): Periodo {
  const n = Number(Array.isArray(valor) ? valor[0] : valor);
  return (PERIODOS as readonly number[]).includes(n) ? (n as Periodo) : 7;
}

const miles = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 });
const compacto = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 1 });

/** 1.284, 12,9 mil, 1,2 M. */
export function numeroCompacto(n: number): string {
  if (n >= 1_000_000) return `${compacto.format(n / 1_000_000)} M`;
  if (n >= 10_000) return `${compacto.format(n / 1_000)} mil`;
  return miles.format(n);
}

export function conUnidad(n: number, clave: ClaveMetrica): string {
  const m = METRICAS[clave];
  return `${miles.format(n)} ${n === 1 ? m.singular : m.plural}`;
}

export type Variacion = { direccion: "sube" | "baja" | "igual"; cambio: string | null; detalle: string };

/**
 * Cambio respecto de los `dias` anteriores, para mostrar junto al número:
 * `cambio` es lo destacado ("+20%") y `detalle`, el resto de la frase.
 */
export function variacion(actual: number, anterior: number, dias: number): Variacion {
  const antes = `${dias} días anteriores`;
  if (actual === anterior) return { direccion: "igual", cambio: null, detalle: `Igual que los ${antes}` };
  if (anterior === 0) return { direccion: "sube", cambio: null, detalle: `Sin actividad los ${antes}` };
  const pct = Math.round(((actual - anterior) / anterior) * 100);
  if (pct === 0) return { direccion: "igual", cambio: null, detalle: `Casi igual que los ${antes}` };
  return { direccion: pct > 0 ? "sube" : "baja", cambio: `${pct > 0 ? "+" : "−"}${Math.abs(pct)}%`, detalle: `vs. ${antes}` };
}

const diaCorto = new Intl.DateTimeFormat("es-AR", { weekday: "short", day: "numeric", month: "short", timeZone: "UTC" });
const diaMes = new Intl.DateTimeFormat("es-AR", { day: "numeric", month: "short", timeZone: "UTC" });

/** "mié, 9 sept" a partir de "2026-09-09" (la fecha ya viene en hora argentina). */
export function fechaCorta(iso: string): string {
  return diaCorto.format(new Date(`${iso}T00:00:00Z`));
}

export function fechaDiaMes(iso: string): string {
  return diaMes.format(new Date(`${iso}T00:00:00Z`));
}
