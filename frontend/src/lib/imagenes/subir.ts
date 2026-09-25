import type { SubidaFirmada } from "@/lib/api/tipos";

/**
 * Sube el archivo directo al almacenamiento con la URL firmada que dio la API. Usa XMLHttpRequest
 * porque fetch todavía no informa el progreso de subida.
 */
export function subirArchivo(
  subida: SubidaFirmada,
  archivo: Blob,
  alProgresar?: (fraccion: number) => void,
): Promise<void> {
  return new Promise((resolver, rechazar) => {
    const pedido = new XMLHttpRequest();
    pedido.open(subida.metodo, subida.url);
    for (const [nombre, valor] of Object.entries(subida.headers)) pedido.setRequestHeader(nombre, valor);
    pedido.upload.onprogress = (e) => {
      if (e.lengthComputable) alProgresar?.(e.loaded / e.total);
    };
    pedido.onload = () =>
      pedido.status >= 200 && pedido.status < 300
        ? resolver()
        : rechazar(new Error(`La subida falló (${pedido.status}).`));
    pedido.onerror = () => rechazar(new Error("No pudimos subir la imagen. Revisá tu conexión."));
    pedido.send(archivo);
  });
}
