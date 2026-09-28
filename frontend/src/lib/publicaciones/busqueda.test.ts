import { describe, expect, it } from "vitest";
import {
  cantidadDeFiltros,
  consultaApi,
  FILTROS_VACIOS,
  formatoDistancia,
  leerFiltros,
  urlDelFeed,
} from "./busqueda";

describe("leerFiltros", () => {
  it("sin parámetros no filtra nada", () => {
    expect(leerFiltros({})).toEqual(FILTROS_VACIOS);
  });

  it("lee la zona con su nombre y un radio por defecto", () => {
    expect(leerFiltros({ zona: "Villa María, Córdoba", lat: "-32.41", lng: "-63.24" }).zona).toEqual({
      nombre: "Villa María, Córdoba",
      lat: -32.41,
      lng: -63.24,
      radioKm: 50,
    });
  });

  it("sin coordenadas no hay zona ni orden por cercanía", () => {
    const f = leerFiltros({ zona: "Rosario", radioKm: "25", orden: "CERCANIA" });
    expect(f.zona).toBeNull();
    expect(f.orden).toBe("RECIENTES");
  });

  it("acepta listas separadas por coma o repetidas y descarta lo desconocido", () => {
    expect(leerFiltros({ carroceria: "SUV,TANQUE" }).carroceria).toEqual(["SUV"]);
    expect(leerFiltros({ carroceria: ["PICKUP", "SUV", "SUV"] }).carroceria).toEqual(["SUV", "PICKUP"]);
  });

  it("ignora valores inválidos", () => {
    const f = leerFiltros({ condicion: "NUEVO", precioMin: "abc", anioMin: "1500", marca: "<script>", moneda: "EUR" });
    expect(f.condicion).toBeNull();
    expect(f.precioMin).toBeNull();
    expect(f.anioMin).toBeNull();
    expect(f.marca).toBeNull();
    expect(f.moneda).toBe("USD");
  });

  it("el modelo requiere marca", () => {
    expect(leerFiltros({ modelo: "hilux" }).modelo).toBeNull();
    expect(leerFiltros({ marca: "toyota", modelo: "hilux" }).modelo).toBe("hilux");
  });

  it("da vuelta rangos invertidos", () => {
    const f = leerFiltros({ precioMin: "30000", precioMax: "10000", anioMin: "2022", anioMax: "2015" });
    expect([f.precioMin, f.precioMax, f.anioMin, f.anioMax]).toEqual([10000, 30000, 2015, 2022]);
  });

  it("solo true activa los filtros de sí/no", () => {
    expect(leerFiltros({ financia: "true", permuta: "1" })).toMatchObject({ financia: true, permuta: false });
  });
});

describe("consultaApi y urlDelFeed", () => {
  const filtros = leerFiltros({
    zona: "Villa María, Córdoba",
    lat: "-32.41",
    lng: "-63.24",
    radioKm: "25",
    condicion: "USADO",
    marca: "toyota",
    modelo: "hilux",
    precioMax: "30000000",
    moneda: "ARS",
    carroceria: "PICKUP,SUV",
    financia: "true",
    orden: "CERCANIA",
  });

  it("arma la consulta de la API sin el nombre de la zona", () => {
    expect(consultaApi(filtros, 2)).toBe(
      "lat=-32.41&lng=-63.24&radioKm=25&condicion=USADO&marca=toyota&modelo=hilux&precioMax=30000000&moneda=ARS" +
        "&carroceria=SUV%2CPICKUP&financia=true&orden=CERCANIA&pagina=2&tamano=24",
    );
  });

  it("la URL de la página se puede volver a leer igual", () => {
    expect(leerFiltros(Object.fromEntries(new URL(urlDelFeed(filtros), "http://x").searchParams))).toEqual(filtros);
  });

  it("sin filtros la URL es la home", () => {
    expect(urlDelFeed(FILTROS_VACIOS)).toBe("/");
    expect(consultaApi(FILTROS_VACIOS)).toBe("tamano=24");
  });

  it("no manda la moneda si no hay precio", () => {
    expect(consultaApi({ ...FILTROS_VACIOS, moneda: "ARS" })).toBe("tamano=24");
  });
});

it("cantidadDeFiltros cuenta cada grupo una vez, sin zona ni orden", () => {
  expect(cantidadDeFiltros(filtrosCon({ precioMin: 1, precioMax: 2, carroceria: ["SUV", "PICKUP"], financia: true }))).toBe(3);
  expect(cantidadDeFiltros(filtrosCon({ orden: "PRECIO_ASC", zona: { nombre: "x", lat: 0, lng: 0, radioKm: 5 } }))).toBe(0);
});

it("formatoDistancia redondea", () => {
  expect(formatoDistancia(0.4)).toBe("a menos de 1 km");
  expect(formatoDistancia(3.24)).toBe("a 3,2 km");
  expect(formatoDistancia(5.0)).toBe("a 5 km");
  expect(formatoDistancia(48.7)).toBe("a 49 km");
});

function filtrosCon(cambios: Partial<typeof FILTROS_VACIOS>) {
  return { ...FILTROS_VACIOS, ...cambios };
}
