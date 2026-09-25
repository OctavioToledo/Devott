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
