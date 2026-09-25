import type { ReactNode } from "react";

/** Clases de inputs y textareas: 48 px de alto y texto de 16 px (evita el zoom en iOS). */
export const claseEntrada =
  "w-full min-h-12 rounded-2xl border border-borde-fuerte bg-superficie px-4 text-base text-tinta " +
  "placeholder:text-secundario/70 aria-invalid:border-marca-oscuro";

/** Ids de ayuda y error para `aria-describedby`. */
export function describir(id: string, ayuda?: ReactNode, error?: string): string | undefined {
  const ids = [ayuda ? `${id}-ayuda` : null, error ? `${id}-error` : null].filter(Boolean);
  return ids.length ? ids.join(" ") : undefined;
}

/**
 * Etiqueta, control, ayuda y error de un campo. El control lo pasa quien lo usa, con
 * `id`, `aria-invalid` y `aria-describedby={describir(id, ayuda, error)}`.
 */
export function Campo({
  id,
  etiqueta,
  ayuda,
  error,
  opcional = false,
  children,
}: {
  id: string;
  etiqueta: string;
  ayuda?: ReactNode;
  error?: string;
  opcional?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        {etiqueta}
        {opcional && <span className="font-normal text-secundario"> (opcional)</span>}
      </label>
      {children}
      {ayuda && (
        <p id={`${id}-ayuda`} className="text-sm text-secundario">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="text-sm font-semibold text-marca-oscuro">
          {error}
        </p>
      )}
    </div>
  );
}
