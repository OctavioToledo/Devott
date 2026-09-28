/** Respuesta de GET /api/v1/me. */
export type Me = {
  id: string;
  email: string | null;
  nombre: string | null;
  avatarUrl: string | null;
  creadoEn: string;
};

/** Respuesta de GET /api/v1/catalogo/marcas. */
export type Marca = {
  id: number;
  nombre: string;
  slug: string;
};

/** Respuesta de GET /api/v1/catalogo/marcas/{id}/modelos. */
export type Modelo = {
  id: number;
  marcaId: number;
  nombre: string;
  slug: string;
};

/** Respuesta de GET /api/v1/ubicaciones. */
export type Localidad = {
  id: string;
  nombre: string;
  provincia: string;
  lat: number;
  lng: number;
};

export type TipoVendedor = "CONCESIONARIA" | "PARTICULAR";

/** Cuerpo de POST y PUT /api/v1/me/vendedor. */
export type VendedorRequest = {
  tipo: TipoVendedor | null;
  nombrePublico: string;
  slug: string;
  /** Código de área + número, sin 0 ni 15. */
  whatsapp: string;
  telefono: string;
  descripcion: string;
  direccion: string;
  localidad: { ciudad: string; provincia: string; lat: number; lng: number } | null;
  horarios: string;
  instagram: string;
  facebook: string;
};

/** Respuesta de GET /api/v1/me/vendedor. */
export type MiVendedor = {
  id: string;
  tipo: TipoVendedor;
  slug: string;
  nombrePublico: string;
  whatsapp: string;
  telefono: string | null;
  descripcion: string | null;
  direccion: string | null;
  logoUrl: string | null;
  localidad: { ciudad: string; provincia: string; lat: number | null; lng: number | null };
  horarios: string | null;
  instagram: string | null;
  facebook: string | null;
  verificado: boolean;
  creadoEn: string;
};

/** Respuesta de GET /api/v1/vendedores/{slug}. No incluye el WhatsApp. */
export type VendedorPublico = {
  slug: string;
  tipo: TipoVendedor;
  nombrePublico: string;
  logoUrl: string | null;
  descripcion: string | null;
  direccion: string | null;
  ciudad: string | null;
  provincia: string | null;
  horarios: string | null;
  instagram: string | null;
  facebook: string | null;
  verificado: boolean;
  creadoEn: string;
};

/** Respuesta de GET /api/v1/me/vendedor/slug-disponible. */
export type SlugDisponible = {
  slug: string;
  disponible: boolean;
  motivo: string | null;
};

export type Condicion = "0KM" | "USADO";
export type Moneda = "ARS" | "USD";
export type Carroceria =
  | "SEDAN"
  | "HATCHBACK"
  | "SUV"
  | "PICKUP"
  | "COUPE"
  | "CONVERTIBLE"
  | "RURAL"
  | "MONOVOLUMEN"
  | "UTILITARIO";
export type Combustible = "NAFTA" | "DIESEL" | "GNC" | "HIBRIDO" | "ELECTRICO";
export type Transmision = "MANUAL" | "AUTOMATICA";
export type Traccion = "DELANTERA" | "TRASERA" | "4X4" | "AWD";
export type EstadoPublicacion = "BORRADOR" | "ACTIVA" | "PAUSADA" | "VENDIDA";

/** Cuerpo de POST y PUT /api/v1/me/publicaciones. */
export type PublicacionRequest = {
  modeloId: number | null;
  version: string;
  anio: number | null;
  km: number | null;
  condicion: Condicion | null;
  precio: number | null;
  moneda: Moneda;
  carroceria: Carroceria | null;
  combustible: Combustible | null;
  transmision: Transmision | null;
  traccion: Traccion | null;
  color: string;
  puertas: number | null;
  financia: boolean;
  aceptaPermuta: boolean;
  unicoDueno: boolean;
  descripcion: string;
  /** null: se usa la ubicación del vendedor. */
  localidad: { ciudad: string; provincia: string; lat: number; lng: number } | null;
};

type DatosComunesPublicacion = {
  slug: string;
  estado: EstadoPublicacion;
  titulo: string;
  modelo: { marcaId: number; marca: string; modeloId: number; modelo: string };
  version: string | null;
  anio: number;
  km: number;
  condicion: Condicion;
  precio: number;
  moneda: Moneda;
  carroceria: Carroceria | null;
  combustible: Combustible | null;
  transmision: Transmision | null;
  traccion: Traccion | null;
  color: string | null;
  puertas: number | null;
  financia: boolean;
  aceptaPermuta: boolean;
  unicoDueno: boolean;
  descripcion: string | null;
  publicadaEn: string | null;
};

export type Foto = {
  id: string;
  url: string;
  orden: number;
  ancho: number | null;
  alto: number | null;
};

/** Respuesta de los endpoints url-subida: el navegador sube el archivo directo con estos datos. */
export type SubidaFirmada = {
  ruta: string;
  url: string;
  metodo: string;
  headers: Record<string, string>;
  venceEn: string;
};

/** Respuesta de /api/v1/me/publicaciones. */
export type MiPublicacion = DatosComunesPublicacion & {
  id: string;
  localidad: { ciudad: string | null; provincia: string | null; lat: number | null; lng: number | null };
  fotos: Foto[];
  /** Máximo de fotos que permite el plan. */
  maxFotos: number;
  vendidaEn: string | null;
  creadaEn: string;
  actualizadaEn: string;
};

/** Respuesta de GET /api/v1/publicaciones/{slug}. */
export type PublicacionPublica = DatosComunesPublicacion & {
  ciudad: string | null;
  provincia: string | null;
  fotos: Foto[];
  vendedor: {
    slug: string;
    nombrePublico: string;
    tipo: TipoVendedor;
    verificado: boolean;
    ciudad: string | null;
    provincia: string | null;
  };
};

/** Resumen para listados (stock del perfil y feed). */
export type TarjetaPublicacion = {
  slug: string;
  estado: EstadoPublicacion;
  titulo: string;
  version: string | null;
  anio: number;
  km: number;
  condicion: Condicion;
  precio: number;
  moneda: Moneda;
  financia: boolean;
  ciudad: string | null;
  provincia: string | null;
  /** URL de la primera foto, o null si no tiene. */
  portada: string | null;
  cantidadFotos: number;
  /** Distancia en km al centro de la búsqueda. Solo en el feed, si se buscó por zona. */
  distanciaKm?: number | null;
};

export type OrdenBusqueda = "RECIENTES" | "PRECIO_ASC" | "PRECIO_DESC" | "KM_ASC" | "ANIO_DESC" | "CERCANIA";

export type Pagina<T> = {
  items: T[];
  pagina: number;
  tamano: number;
  total: number;
  hayMas: boolean;
};

/** Respuesta de GET /api/v1/me/seguimientos. */
export type VendedorSeguido = {
  slug: string;
  nombrePublico: string;
  tipo: TipoVendedor;
  verificado: boolean;
  logoUrl: string | null;
  ciudad: string | null;
  provincia: string | null;
  autosActivos: number;
};
