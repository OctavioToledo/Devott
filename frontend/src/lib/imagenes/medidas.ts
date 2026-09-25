/** Achica ancho y alto para que ninguno pase del máximo, manteniendo la proporción. No agranda. */
export function medidasAjustadas(ancho: number, alto: number, maximo: number): { ancho: number; alto: number } {
  const escala = Math.min(1, maximo / Math.max(ancho, alto));
  return { ancho: Math.max(1, Math.round(ancho * escala)), alto: Math.max(1, Math.round(alto * escala)) };
}
