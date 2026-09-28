import { expect, it } from "vitest";
import { fechaDelPlan, linkParaPedirPlan, precioMensual } from "./formato";

const plan = { codigo: "CONCESIONARIA", nombre: "Concesionaria", maxPublicaciones: 20, maxFotos: 8, precioArs: 35000 };

it("precioMensual con puntos de miles", () => {
  expect(precioMensual(plan)).toBe("$ 35.000");
});

it("fechaDelPlan sin correrse de día", () => {
  expect(fechaDelPlan("2026-10-01")).toBe("1 de octubre");
});

it("linkParaPedirPlan arma el mensaje con el perfil", () => {
  const link = new URL(linkParaPedirPlan("5493534000000", plan, "https://devott.com/autos-sur"));
  expect(link.pathname).toBe("/5493534000000");
  expect(link.searchParams.get("text")).toBe(
    "¡Hola! Quiero contratar el plan Concesionaria de Devott ($ 35.000 por mes). Mi perfil: https://devott.com/autos-sur",
  );
});
