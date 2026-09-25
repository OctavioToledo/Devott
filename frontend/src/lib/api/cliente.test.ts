import { afterEach, describe, expect, it, vi } from "vitest";
import { ErrorApi, pedirApi } from "./cliente";

function responder(respuesta: Response) {
  const fetchFalso = vi.fn().mockResolvedValue(respuesta);
  vi.stubGlobal("fetch", fetchFalso);
  return fetchFalso;
}

function problema(status: number, cuerpo: object) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("pedirApi", () => {
  it("manda el token y devuelve el JSON", async () => {
    const fetchFalso = responder(Response.json({ id: "1" }));

    await expect(pedirApi("/me", { token: "abc" })).resolves.toEqual({ id: "1" });

    const [url, init] = fetchFalso.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/me");
    expect(init.headers.Authorization).toBe("Bearer abc");
    expect(init.body).toBeUndefined();
  });

  it("serializa el cuerpo como JSON", async () => {
    const fetchFalso = responder(new Response(null, { status: 204 }));

    await expect(pedirApi("/me/guardados", { metodo: "POST", cuerpo: { id: 7 } })).resolves.toBeUndefined();

    const [, init] = fetchFalso.mock.calls[0];
    expect(init.method).toBe("POST");
    expect(init.headers["Content-Type"]).toBe("application/json");
    expect(init.body).toBe('{"id":7}');
  });

  it("convierte un ProblemDetail en ErrorApi con los errores por campo", async () => {
    responder(
      problema(400, {
        title: "Solicitud inválida",
        detail: "Hay datos inválidos en la solicitud.",
        errores: [{ campo: "nombre", mensaje: "no debe estar vacío" }],
      }),
    );

    const error = await pedirApi("/me/vendedor").catch((e) => e);

    expect(error).toBeInstanceOf(ErrorApi);
    expect(error).toMatchObject({
      status: 400,
      titulo: "Solicitud inválida",
      detalle: "Hay datos inválidos en la solicitud.",
      errores: [{ campo: "nombre", mensaje: "no debe estar vacío" }],
    });
  });

  it("usa un mensaje genérico si la respuesta de error no es JSON", async () => {
    responder(new Response("<html>502</html>", { status: 502, headers: { "Content-Type": "text/html" } }));

    await expect(pedirApi("/me")).rejects.toMatchObject({
      status: 502,
      detalle: "Algo salió mal. Probá de nuevo en un rato.",
    });
  });

  it("avisa cuando no hay conexión con el servidor", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("fetch failed")));

    await expect(pedirApi("/me")).rejects.toMatchObject({ status: 0, titulo: "Sin conexión" });
  });
});
