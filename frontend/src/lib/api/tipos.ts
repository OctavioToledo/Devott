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
