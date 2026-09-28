import { describe, expect, it } from "vitest";
import { conUnidad, fechaDiaMes, leerPeriodo, numeroCompacto, variacion } from "./formato";

describe("variacion", () => {
  it("porcentaje con signo contra el período anterior", () => {
    expect(variacion(12, 10, 7)).toEqual({ direccion: "sube", cambio: "+20%", detalle: "vs. 7 días anteriores" });
    expect(variacion(5, 10, 30)).toEqual({ direccion: "baja", cambio: "−50%", detalle: "vs. 30 días anteriores" });
  });

  it("casos sin porcentaje", () => {
    expect(variacion(0, 0, 7)).toEqual({ direccion: "igual", cambio: null, detalle: "Igual que los 7 días anteriores" });
    expect(variacion(3, 0, 7)).toEqual({ direccion: "sube", cambio: null, detalle: "Sin actividad los 7 días anteriores" });
    expect(variacion(1001, 1000, 7).detalle).toBe("Casi igual que los 7 días anteriores");
  });
});

it("numeroCompacto", () => {
  expect(numeroCompacto(1284)).toBe("1.284");
  expect(numeroCompacto(12900)).toBe("12,9 mil");
  expect(numeroCompacto(1_250_000)).toBe("1,3 M");
});

it("conUnidad usa singular y plural", () => {
  expect(conUnidad(1, "contactos")).toBe("1 clic");
  expect(conUnidad(1500, "vistasPerfil")).toBe("1.500 visitas");
});

it("leerPeriodo acepta 7, 30 y 90", () => {
  expect(leerPeriodo("30")).toBe(30);
  expect(leerPeriodo("5")).toBe(7);
  expect(leerPeriodo(undefined)).toBe(7);
});

it("fechaDiaMes no se corre de día por la zona horaria", () => {
  expect(fechaDiaMes("2026-09-01")).toMatch(/^1 sept?/);
});
