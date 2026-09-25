/** URL pública del sitio, para metadatos y links absolutos (Open Graph, callbacks de login). */
export function urlDelSitio(): string {
  return process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
}

/** URL base de la API de Spring, con /api/v1 incluido. */
export function urlDeLaApi(): string {
  return process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";
}
