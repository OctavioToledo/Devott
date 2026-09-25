import { describe, expect, it } from "vitest";
import { digitosOdometro, formatoKm, formatoPrecio, numeroConPuntos } from "./etiquetas";

describe("formatoPrecio", () => {
  it("usa el símbolo de la moneda y puntos de miles", () => {
    expect(formatoPrecio(34900, "USD")).toBe("US$ 34.900");
    expect(formatoPrecio(27450000, "ARS")).toBe("$ 27.450.000");
  });
});

it("formatoKm usa puntos de miles", () => {
  expect(formatoKm(135700)).toBe("135.700");
});

describe("digitosOdometro", () => {
  it("completa con ceros a la izquierda", () => {
    expect(digitosOdometro(68400)).toEqual(["0", "6", "8", "4", "0", "0"]);
    expect(digitosOdometro(0)).toEqual(["0", "0", "0", "0", "0", "0"]);
  });

  it("no corta kilometrajes de más de seis dígitos", () => {
    expect(digitosOdometro(1234567)).toHaveLength(7);
  });
});

describe("numeroConPuntos", () => {
  it("se queda con los dígitos y los formatea", () => {
    expect(numeroConPuntos("34900")).toEqual({ valor: 34900, mostrado: "34.900" });
    expect(numeroConPuntos("US$ 1.250.000")).toEqual({ valor: 1250000, mostrado: "1.250.000" });
  });

  it("devuelve vacío si no hay dígitos", () => {
    expect(numeroConPuntos("abc")).toEqual({ valor: null, mostrado: "" });
  });
});
