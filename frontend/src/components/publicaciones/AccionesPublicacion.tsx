"use client";

import { useRouter } from "next/navigation";
import { useRef, useState, useTransition } from "react";
import { Boton, BotonLink } from "@/components/ui/Boton";
import type { EstadoPublicacion } from "@/lib/api/tipos";
import { cambiarEstadoPublicacion, eliminarPublicacion } from "@/lib/publicaciones/acciones";

/** Cambios de estado que se ofrecen según el estado actual. */
const ACCIONES: Record<EstadoPublicacion, { estado: EstadoPublicacion; texto: string }[]> = {
  BORRADOR: [{ estado: "ACTIVA", texto: "Publicar" }],
  ACTIVA: [
    { estado: "PAUSADA", texto: "Pausar" },
    { estado: "VENDIDA", texto: "Vendido" },
  ],
  PAUSADA: [
    { estado: "ACTIVA", texto: "Reactivar" },
    { estado: "VENDIDA", texto: "Vendido" },
  ],
  VENDIDA: [{ estado: "ACTIVA", texto: "Volver a publicar" }],
};

export function AccionesPublicacion({
  id,
  titulo,
  estado,
  enEdicion = false,
}: {
  id: string;
  titulo: string;
  estado: EstadoPublicacion;
  /** Dentro de la página de edición: sin botón "Editar" y, al eliminar, vuelve al panel. */
  enEdicion?: boolean;
}) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pendiente, iniciar] = useTransition();
  const dialogo = useRef<HTMLDialogElement>(null);

  function cambiar(nuevo: EstadoPublicacion) {
    setError(null);
    iniciar(async () => setError(await cambiarEstadoPublicacion(id, nuevo)));
  }

  function eliminar() {
    dialogo.current?.close();
    setError(null);
    iniciar(async () => {
      const fallo = await eliminarPublicacion(id);
      setError(fallo);
      if (!fallo && enEdicion) router.push("/panel");
    });
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        {!enEdicion && (
          <BotonLink href={`/panel/publicaciones/${id}`} variante="suave">
            Editar
          </BotonLink>
        )}
        {ACCIONES[estado].map((a) => (
          <Boton
            key={a.estado}
            variante={a.estado === "ACTIVA" ? "marca" : "suave"}
            disabled={pendiente}
            onClick={() => cambiar(a.estado)}
          >
            {a.texto}
          </Boton>
        ))}
        <Boton variante="suave" disabled={pendiente} onClick={() => dialogo.current?.showModal()}>
          Eliminar
        </Boton>
      </div>
      {error && (
        <p role="alert" className="text-sm font-semibold text-marca-oscuro">
          {error}
        </p>
      )}

      <dialog
        ref={dialogo}
        aria-labelledby={`eliminar-${id}`}
        className="m-auto w-[min(26rem,calc(100%-2rem))] rounded-[22px] bg-superficie p-6 text-tinta backdrop:bg-tinta/50"
      >
        <h2 id={`eliminar-${id}`} className="font-titulo text-xl font-extrabold">
          ¿Eliminar {titulo}?
        </h2>
        <p className="mt-2 text-sm text-secundario">
          Se borran la publicación y sus fotos. No se puede deshacer. Si ya la vendiste, mejor marcala como vendida.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Boton variante="suave" onClick={() => dialogo.current?.close()}>
            Cancelar
          </Boton>
          <Boton variante="oscuro" onClick={eliminar}>
            Eliminar
          </Boton>
        </div>
      </dialog>
    </div>
  );
}
