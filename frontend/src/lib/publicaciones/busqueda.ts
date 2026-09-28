import type {
  Carroceria,
  Combustible,
  Condicion,
  Moneda,
  OrdenBusqueda,
  TipoVendedor,
  Transmision,
} from "@/lib/api/tipos";
import { CARROCERIAS, COMBUSTIBLES, TRANSMISIONES } from "./etiquetas";

export type Zona = { nombre: string; lat: number; lng: number; radioKm: number };

/** Filtros del feed. La URL de la página usa los mismos nombres que la API, más `zona` (el nombre del lugar). */
export type FiltrosFeed = {
  zona: Zona | null;
  condicion: Condicion | null;
  tipoVendedor: TipoVendedor | null;
  marca: string | null;
  modelo: string | null;
  precioMin: number | null;
  precioMax: number | null;
  moneda: Moneda;
  anioMin: number | null;
  anioMax: number | null;
  kmMax: number | null;
  carroceria: Carroceria[];
  combustible: Combustible[];
  transmision: Transmision[];
  financia: boolean;
  permuta: boolean;
  unicoDueno: boolean;
  orden: OrdenBusqueda;
};

export type ParametrosUrl = Record<string, string | string[] | undefined>;

export const RADIOS_KM = [10, 25, 50, 100, 200] as const;
export const RADIO_POR_DEFECTO = 50;
export const TAMANO_PAGINA = 24;

export const ORDENES: Record<OrdenBusqueda, string> = {
  RECIENTES: "Más recientes",
  CERCANIA: "Más cercanos",
  PRECIO_ASC: "Menor precio",
  PRECIO_DESC: "Mayor precio",
  KM_ASC: "Menos kilómetros",
  ANIO_DESC: "Más nuevos",
};

export const FILTROS_VACIOS: FiltrosFeed = {
  zona: null,
  condicion: null,
  tipoVendedor: null,
  marca: null,
  modelo: null,
  precioMin: null,
  precioMax: null,
  moneda: "USD",
  anioMin: null,
  anioMax: null,
  kmMax: null,
  carroceria: [],
  combustible: [],
  transmision: [],
  financia: false,
  permuta: false,
  unicoDueno: false,
  orden: "RECIENTES",
};

function uno(p: ParametrosUrl, clave: string): string | null {
  const v = p[clave];
  const texto = (Array.isArray(v) ? v[0] : v)?.trim();
  return texto ? texto : null;
}

function numero(p: ParametrosUrl, clave: string, min: number, max: number): number | null {
  const texto = uno(p, clave);
  if (texto === null) return null;
  const n = Number(texto);
  return Number.isFinite(n) && n >= min && n <= max ? n : null;
}

function entero(p: ParametrosUrl, clave: string, min: number, max: number): number | null {
  const n = numero(p, clave, min, max);
  return n === null ? null : Math.round(n);
}

function deLista<T extends string>(p: ParametrosUrl, clave: string, validos: readonly T[]): T | null {
  const texto = uno(p, clave);
  return texto !== null && (validos as readonly string[]).includes(texto) ? (texto as T) : null;
}

/** Acepta `carroceria=SUV&carroceria=PICKUP` y `carroceria=SUV,PICKUP`; descarta valores desconocidos y repetidos. */
function varios<T extends string>(p: ParametrosUrl, clave: string, validos: readonly T[]): T[] {
  const v = p[clave];
  const valores = (Array.isArray(v) ? v : v ? [v] : []).flatMap((x) => x.split(",")).map((x) => x.trim());
  return validos.filter((valido) => valores.includes(valido));
}

function slug(p: ParametrosUrl, clave: string): string | null {
  const texto = uno(p, clave);
  return texto !== null && /^[a-z0-9]+(-[a-z0-9]+)*$/.test(texto) ? texto : null;
}

const si = (p: ParametrosUrl, clave: string) => uno(p, clave) === "true";

/** Lee los filtros de la URL de la página. Lo inválido se ignora en lugar de romper la búsqueda. */
export function leerFiltros(p: ParametrosUrl): FiltrosFeed {
  const lat = numero(p, "lat", -90, 90);
  const lng = numero(p, "lng", -180, 180);
  const radioKm = entero(p, "radioKm", 1, 1000) ?? RADIO_POR_DEFECTO;
  const zona = lat !== null && lng !== null ? { nombre: uno(p, "zona") ?? "Tu ubicación", lat, lng, radioKm } : null;

  const marca = slug(p, "marca");
  let orden = deLista(p, "orden", Object.keys(ORDENES) as OrdenBusqueda[]) ?? "RECIENTES";
  if (orden === "CERCANIA" && !zona) orden = "RECIENTES";

  let precioMin = numero(p, "precioMin", 1, 1e12);
  let precioMax = numero(p, "precioMax", 1, 1e12);
  if (precioMin !== null && precioMax !== null && precioMin > precioMax) [precioMin, precioMax] = [precioMax, precioMin];
  let anioMin = entero(p, "anioMin", 1900, 2100);
  let anioMax = entero(p, "anioMax", 1900, 2100);
  if (anioMin !== null && anioMax !== null && anioMin > anioMax) [anioMin, anioMax] = [anioMax, anioMin];

  return {
    zona,
    condicion: deLista(p, "condicion", ["0KM", "USADO"] as const),
    tipoVendedor: deLista(p, "tipoVendedor", ["CONCESIONARIA", "PARTICULAR"] as const),
    marca,
    modelo: marca ? slug(p, "modelo") : null,
    precioMin,
    precioMax,
    moneda: deLista(p, "moneda", ["ARS", "USD"] as const) ?? "USD",
    anioMin,
    anioMax,
    kmMax: entero(p, "kmMax", 0, 10_000_000),
    carroceria: varios(p, "carroceria", Object.keys(CARROCERIAS) as Carroceria[]),
    combustible: varios(p, "combustible", Object.keys(COMBUSTIBLES) as Combustible[]),
    transmision: varios(p, "transmision", Object.keys(TRANSMISIONES) as Transmision[]),
    financia: si(p, "financia"),
    permuta: si(p, "permuta"),
    unicoDueno: si(p, "unicoDueno"),
    orden,
  };
}

/** Parámetros comunes a la URL de la página y a la API. Solo incluye lo que filtra. */
function parametros(f: FiltrosFeed): URLSearchParams {
  const q = new URLSearchParams();
  const poner = (clave: string, valor: string | number | null | boolean) => {
    if (valor !== null && valor !== false && valor !== "") q.set(clave, String(valor));
  };
  if (f.zona) {
    poner("lat", f.zona.lat);
    poner("lng", f.zona.lng);
    poner("radioKm", f.zona.radioKm);
  }
  poner("condicion", f.condicion);
  poner("tipoVendedor", f.tipoVendedor);
  poner("marca", f.marca);
  poner("modelo", f.marca ? f.modelo : null);
  poner("precioMin", f.precioMin);
  poner("precioMax", f.precioMax);
  if (f.precioMin !== null || f.precioMax !== null) poner("moneda", f.moneda === "USD" ? null : f.moneda);
  poner("anioMin", f.anioMin);
  poner("anioMax", f.anioMax);
  poner("kmMax", f.kmMax);
  poner("carroceria", f.carroceria.join(","));
  poner("combustible", f.combustible.join(","));
  poner("transmision", f.transmision.join(","));
  poner("financia", f.financia);
  poner("permuta", f.permuta);
  poner("unicoDueno", f.unicoDueno);
  poner("orden", f.orden === "RECIENTES" ? null : f.orden);
  return q;
}

/** Query string para la API (sin "?"). */
export function consultaApi(f: FiltrosFeed, pagina = 0, tamano = TAMANO_PAGINA): string {
  const q = parametros(f);
  if (pagina > 0) q.set("pagina", String(pagina));
  q.set("tamano", String(tamano));
  return q.toString();
}

/** URL de la página del feed con estos filtros. La zona se guarda con su nombre para mostrarlo. */
export function urlDelFeed(f: FiltrosFeed): string {
  const q = parametros(f);
  if (f.zona) q.set("zona", f.zona.nombre);
  const texto = q.toString();
  return texto ? `/?${texto}` : "/";
}

/** Cuántos filtros hay aplicados, sin contar zona ni orden (se muestran aparte). */
export function cantidadDeFiltros(f: FiltrosFeed): number {
  return [
    f.condicion,
    f.tipoVendedor,
    f.marca,
    f.precioMin !== null || f.precioMax !== null ? true : null,
    f.anioMin !== null || f.anioMax !== null ? true : null,
    f.kmMax,
    f.carroceria.length ? true : null,
    f.combustible.length ? true : null,
    f.transmision.length ? true : null,
    f.financia || null,
    f.permuta || null,
    f.unicoDueno || null,
  ].filter((x) => x !== null).length;
}

/** "a 12 km", "a menos de 1 km". */
export function formatoDistancia(km: number): string {
  if (km < 1) return "a menos de 1 km";
  return `a ${km < 10 ? km.toFixed(1).replace(".", ",").replace(",0", "") : Math.round(km)} km`;
}
