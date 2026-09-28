/** Rutas que exigen sesión. El proxy manda a /ingresar a quien entre sin estar logueado. */
export const RUTAS_PRIVADAS = ["/cuenta", "/panel", "/guardados"];

export function esRutaPrivada(ruta: string): boolean {
  return RUTAS_PRIVADAS.some((privada) => ruta === privada || ruta.startsWith(`${privada}/`));
}

/**
 * Valida el destino al que volver después del login. Solo acepta rutas internas,
 * para que nadie pueda usar el login para redirigir a otro sitio.
 */
export function destinoSeguro(destino: string | null | undefined): string {
  if (!destino || !destino.startsWith("/") || destino.startsWith("//") || destino.includes("\\")) {
    return "/";
  }
  return destino;
}

export function urlDeIngreso(destino: string): string {
  return `/ingresar?siguiente=${encodeURIComponent(destino)}`;
}
