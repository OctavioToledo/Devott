import { expect, it } from "vitest";
import { medidasAjustadas } from "./medidas";

it("achica fotos horizontales y verticales por el lado más largo", () => {
  expect(medidasAjustadas(4000, 3000, 1600)).toEqual({ ancho: 1600, alto: 1200 });
  expect(medidasAjustadas(3000, 4000, 1600)).toEqual({ ancho: 1200, alto: 1600 });
});

it("no agranda fotos chicas", () => {
  expect(medidasAjustadas(800, 600, 1600)).toEqual({ ancho: 800, alto: 600 });
});
