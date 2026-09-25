/** Respuesta de GET /api/v1/me. */
export type Me = {
  id: string;
  email: string | null;
  nombre: string | null;
  avatarUrl: string | null;
  creadoEn: string;
};
