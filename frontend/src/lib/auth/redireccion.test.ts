import { describe, expect, it } from "vitest";
import { destinoSeguro, esRutaPrivada, urlDeIngreso } from "./redireccion";

describe("destinoSeguro", () => {
  it("acepta rutas internas", () => {
    expect(destinoSeguro("/cuenta")).toBe("/cuenta");
    expect(destinoSeguro("/panel?tab=fotos")).toBe("/panel?tab=fotos");
  });

  it("manda al inicio si falta el destino", () => {
    expect(destinoSeguro(null)).toBe("/");
    expect(destinoSeguro("")).toBe("/");
  });

  it("rechaza destinos a otros sitios", () => {
    expect(destinoSeguro("https://otro.com")).toBe("/");
    expect(destinoSeguro("//otro.com")).toBe("/");
    expect(destinoSeguro("/\\otro.com")).toBe("/");
  });
});

describe("esRutaPrivada", () => {
  it("guardados es privada", () => {
    expect(esRutaPrivada("/guardados")).toBe(true);
  });

  it("reconoce las rutas privadas y sus subrutas", () => {
    expect(esRutaPrivada("/cuenta")).toBe(true);
    expect(esRutaPrivada("/panel/publicaciones")).toBe(true);
  });

  it("no confunde rutas públicas que empiezan igual", () => {
    expect(esRutaPrivada("/")).toBe(false);
    expect(esRutaPrivada("/panelistas")).toBe(false);
  });
});

it("urlDeIngreso codifica el destino", () => {
  expect(urlDeIngreso("/panel?tab=fotos")).toBe("/ingresar?siguiente=%2Fpanel%3Ftab%3Dfotos");
});
