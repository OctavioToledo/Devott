import type {
  Carroceria,
  Combustible,
  Condicion,
  EstadoPublicacion,
  Moneda,
  Traccion,
  Transmision,
} from "@/lib/api/tipos";

export const CONDICIONES: Record<Condicion, string> = { "0KM": "0 km", USADO: "Usado" };

export const CARROCERIAS: Record<Carroceria, string> = {
  SEDAN: "Sedán",
  HATCHBACK: "Hatchback",
  SUV: "SUV",
  PICKUP: "Pickup",
  COUPE: "Coupé",
  CONVERTIBLE: "Convertible",
  RURAL: "Rural",
  MONOVOLUMEN: "Monovolumen",
  UTILITARIO: "Utilitario",
};

export const COMBUSTIBLES: Record<Combustible, string> = {
  NAFTA: "Nafta",
  DIESEL: "Diésel",
  GNC: "GNC",
  HIBRIDO: "Híbrido",
  ELECTRICO: "Eléctrico",
};

export const TRANSMISIONES: Record<Transmision, string> = { MANUAL: "Manual", AUTOMATICA: "Automática" };

export const TRACCIONES: Record<Traccion, string> = {
  DELANTERA: "Delantera",
  TRASERA: "Trasera",
  "4X4": "4x4",
  AWD: "Integral (AWD)",
};

export const ESTADOS: Record<EstadoPublicacion, string> = {
  BORRADOR: "Borrador",
  ACTIVA: "Activa",
  PAUSADA: "Pausada",
  VENDIDA: "Vendida",
};

export const MONEDAS: Record<Moneda, string> = { ARS: "$", USD: "US$" };

const miles = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 0 });

/** "US$ 34.900" o "$ 27.450.000". */
export function formatoPrecio(precio: number, moneda: Moneda): string {
  return `${MONEDAS[moneda]} ${miles.format(precio)}`;
}

/** "68.400". */
export function formatoKm(km: number): string {
  return miles.format(km);
}

/** Dígitos del odómetro, con ceros a la izquierda: 68400 → ["0","6","8","4","0","0"]. */
export function digitosOdometro(km: number, largo = 6): string[] {
  const texto = String(Math.max(0, Math.round(km)));
  return texto.padStart(Math.max(largo, texto.length), "0").split("");
}

/** Solo los dígitos de lo que se escribe en un campo numérico, formateado con puntos: "34900" → "34.900". */
export function numeroConPuntos(texto: string): { valor: number | null; mostrado: string } {
  const digitos = texto.replace(/\D/g, "").replace(/^0+(?=\d)/, "").slice(0, 12);
  if (!digitos) return { valor: null, mostrado: "" };
  const valor = Number(digitos);
  return { valor, mostrado: miles.format(valor) };
}
