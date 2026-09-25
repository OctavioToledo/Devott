import { medidasAjustadas } from "./medidas";

export type ImagenComprimida = { archivo: Blob; tipo: "image/webp" | "image/jpeg"; ancho: number; alto: number };

/**
 * Redimensiona y comprime una imagen en el navegador antes de subirla: el lado más largo queda en
 * `maximo` píxeles y se convierte a WebP (o a JPEG en los navegadores que no generan WebP).
 * Respeta la orientación de las fotos del celular.
 */
export async function comprimirImagen(archivo: File, maximo = 1600, calidad = 0.82): Promise<ImagenComprimida> {
  const bitmap = await createImageBitmap(archivo, { imageOrientation: "from-image" });
  try {
    const { ancho, alto } = medidasAjustadas(bitmap.width, bitmap.height, maximo);
    const lienzo = document.createElement("canvas");
    lienzo.width = ancho;
    lienzo.height = alto;
    const contexto = lienzo.getContext("2d");
    if (!contexto) throw new Error("El navegador no puede procesar imágenes.");
    contexto.imageSmoothingQuality = "high";
    contexto.drawImage(bitmap, 0, 0, ancho, alto);

    const webp = await aBlob(lienzo, "image/webp", calidad);
    if (webp?.type === "image/webp") return { archivo: webp, tipo: "image/webp", ancho, alto };
    const jpeg = await aBlob(lienzo, "image/jpeg", calidad);
    if (!jpeg) throw new Error("No pudimos procesar la imagen.");
    return { archivo: jpeg, tipo: "image/jpeg", ancho, alto };
  } finally {
    bitmap.close();
  }
}

function aBlob(lienzo: HTMLCanvasElement, tipo: string, calidad: number): Promise<Blob | null> {
  return new Promise((resolver) => lienzo.toBlob(resolver, tipo, calidad));
}
