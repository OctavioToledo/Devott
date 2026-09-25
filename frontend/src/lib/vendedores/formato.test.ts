import { describe, expect, it } from "vitest";
import { iniciales, limpiarSlugMientrasSeEscribe, slugDesde, urlVisibleDelPerfil, whatsappLocal } from "./formato";

describe("slugDesde", () => {
  it("genera el mismo slug que el backend", () => {
    expect(slugDesde("Automotores del Sur")).toBe("automotores-del-sur");
    expect(slugDesde("  Peña & Hnos. S.R.L. ")).toBe("pena-hnos-s-r-l");
    expect(slugDesde("Citroën 2008!!")).toBe("citroen-2008");
  });

  it("corta en 40 caracteres sin dejar un guion al final", () => {
    const slug = slugDesde("Concesionaria Oficial de Automotores del Centro de la Provincia");
    expect(slug.length).toBeLessThanOrEqual(40);
    expect(slug.endsWith("-")).toBe(false);
  });
});

it("limpiarSlugMientrasSeEscribe permite seguir escribiendo después de un guion", () => {
  expect(limpiarSlugMientrasSeEscribe("Autos Sur-")).toBe("autos-sur-");
  expect(limpiarSlugMientrasSeEscribe("--autos__sur")).toBe("autos-sur");
});

describe("whatsappLocal", () => {
  it("acepta los mismos formatos que el backend", () => {
    expect(whatsappLocal("3534 123456")).toBe("3534123456");
    expect(whatsappLocal("0353 4123456")).toBe("3534123456");
    expect(whatsappLocal("+54 9 353 412-3456")).toBe("3534123456");
  });

  it("rechaza números incompletos o con 15", () => {
    expect(whatsappLocal("4123456")).toBeNull();
    expect(whatsappLocal("0353 15 4123456")).toBeNull();
  });
});

describe("iniciales", () => {
  it("usa la primera y la última palabra, salteando conectores", () => {
    expect(iniciales("Automotores del Sur")).toBe("AS");
    expect(iniciales("Martín")).toBe("M");
    expect(iniciales("")).toBe("?");
  });
});

it("urlVisibleDelPerfil saca el protocolo", () => {
  expect(urlVisibleDelPerfil("autos-sur")).toBe("localhost:3000/autos-sur");
});
