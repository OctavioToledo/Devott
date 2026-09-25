"use client";

import Image from "next/image";
import { useId, useRef, useState } from "react";
import { Boton } from "@/components/ui/Boton";
import { clasesBoton } from "@/components/ui/boton";
import { comprimirImagen } from "@/lib/imagenes/comprimir";
import { subirArchivo } from "@/lib/imagenes/subir";
import { guardarLogo, pedirSubidaLogo } from "@/lib/vendedores/acciones";
import { iniciales } from "@/lib/vendedores/formato";

export function GestorLogo({ nombre, logoInicial }: { nombre: string; logoInicial: string | null }) {
  const idEntrada = useId();
  const entrada = useRef<HTMLInputElement>(null);
  const [logo, setLogo] = useState(logoInicial);
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function subir(archivo: File | undefined) {
    if (!archivo) return;
    setError(null);
    setOcupado(true);
    try {
      const imagen = await comprimirImagen(archivo, 512, 0.9);
      const pedido = await pedirSubidaLogo(imagen.tipo);
      if (!pedido.ok) throw new Error(pedido.mensaje);
      await subirArchivo(pedido.subida, imagen.archivo);
      const resultado = await guardarLogo(pedido.subida.ruta);
      if (!resultado.ok) throw new Error(resultado.mensaje);
      setLogo(resultado.vendedor.logoUrl);
    } catch (e) {
      setError(e instanceof Error ? e.message : "No pudimos subir el logo.");
    } finally {
      setOcupado(false);
      if (entrada.current) entrada.current.value = "";
    }
  }

  async function quitar() {
    setError(null);
    setOcupado(true);
    const resultado = await guardarLogo(null);
    setOcupado(false);
    if (resultado.ok) setLogo(null);
    else setError(resultado.mensaje);
  }

  return (
    <section aria-labelledby="titulo-logo" className="flex flex-col gap-3">
      <h2 id="titulo-logo" className="text-sm font-semibold">
        Logo <span className="font-normal text-secundario">(opcional)</span>
      </h2>
      <div className="flex items-center gap-4">
        <div className="font-titulo relative flex size-[72px] shrink-0 items-center justify-center overflow-hidden rounded-[20px] bg-marca text-2xl font-extrabold text-white">
          {logo ? (
            <Image src={logo} alt="Tu logo" fill sizes="72px" className="bg-superficie object-contain" unoptimized />
          ) : (
            <span aria-hidden="true">{iniciales(nombre)}</span>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            ref={entrada}
            id={idEntrada}
            type="file"
            accept="image/*"
            disabled={ocupado}
            onChange={(e) => subir(e.target.files?.[0])}
            className="peer sr-only"
          />
          <label
            htmlFor={idEntrada}
            className={clasesBoton(
              "suave",
              "md",
              `peer-focus-visible:outline-2 peer-focus-visible:outline-confianza ${ocupado ? "pointer-events-none opacity-50" : ""}`,
            )}
          >
            {ocupado ? "Subiendo…" : logo ? "Cambiar logo" : "Subir logo"}
          </label>
          {logo && (
            <Boton variante="suave" disabled={ocupado} onClick={quitar}>
              Quitar
            </Boton>
          )}
        </div>
      </div>
      <p className="text-sm text-secundario">Si no subís uno, mostramos tus iniciales.</p>
      {error && (
        <p role="alert" className="text-sm font-semibold text-marca-oscuro">
          {error}
        </p>
      )}
    </section>
  );
}
