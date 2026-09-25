import { urlDeLaApi } from "@/lib/config";

export type ErrorDeCampo = { campo: string; mensaje: string };

/** Error de la API de Spring, armado a partir de su respuesta ProblemDetail (RFC 9457). */
export class ErrorApi extends Error {
  constructor(
    readonly status: number,
    readonly titulo: string,
    readonly detalle: string,
    readonly errores: ErrorDeCampo[] = [],
  ) {
    super(detalle);
    this.name = "ErrorApi";
  }
}

export type OpcionesPedido = {
  metodo?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  cuerpo?: unknown;
  token?: string | null;
  signal?: AbortSignal;
  /** Segundos que Next puede reutilizar la respuesta. Solo para datos públicos; sin esto, no se cachea. */
  revalidar?: number;
};

/**
 * Llama a la API de Spring. `ruta` es relativa a /api/v1 (por ejemplo "/me").
 * Devuelve el JSON de la respuesta, o undefined si viene vacía (204).
 */
export async function pedirApi<T>(ruta: string, opciones: OpcionesPedido = {}): Promise<T> {
  const { metodo = "GET", cuerpo, token, signal, revalidar } = opciones;
  const headers: Record<string, string> = { Accept: "application/json" };
  if (cuerpo !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  let respuesta: Response;
  try {
    respuesta = await fetch(`${urlDeLaApi()}${ruta}`, {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      ...(revalidar === undefined ? { cache: "no-store" as const } : { next: { revalidate: revalidar } }),
      signal,
    });
  } catch (causa) {
    if (signal?.aborted) throw causa;
    throw new ErrorApi(0, "Sin conexión", "No pudimos conectar con el servidor. Probá de nuevo en un rato.");
  }

  if (!respuesta.ok) throw await errorDesdeRespuesta(respuesta);
  if (respuesta.status === 204) return undefined as T;
  const texto = await respuesta.text();
  return (texto ? JSON.parse(texto) : undefined) as T;
}

export async function errorDesdeRespuesta(respuesta: Response): Promise<ErrorApi> {
  const generico = new ErrorApi(respuesta.status, "Error", "Algo salió mal. Probá de nuevo en un rato.");
  const tipo = respuesta.headers.get("Content-Type") ?? "";
  if (!tipo.includes("json")) return generico;

  try {
    const problema = (await respuesta.json()) as {
      title?: string;
      detail?: string;
      errores?: ErrorDeCampo[];
    };
    return new ErrorApi(
      respuesta.status,
      problema.title ?? generico.titulo,
      problema.detail ?? generico.detalle,
      Array.isArray(problema.errores) ? problema.errores : [],
    );
  } catch {
    return generico;
  }
}
