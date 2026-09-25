import { urlDelSitio } from "@/lib/config";

/** "Automotores del Sur" → "automotores-del-sur". Mismo criterio que el backend. */
export function slugDesde(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 40)
    .replace(/-+$/, "");
}

/** Mientras se escribe el link: minúsculas, sin acentos y con guiones, pero deja el guion final. */
export function limpiarSlugMientrasSeEscribe(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9-]+/g, "-")
    .replace(/-{2,}/g, "-")
    .replace(/^-+/, "")
    .slice(0, 40);
}

/**
 * Normaliza el WhatsApp como el backend y devuelve el número local (código de área + número),
 * o null si no se puede. Sirve para avisar antes de enviar el formulario.
 */
export function whatsappLocal(texto: string): string | null {
  let digitos = texto.replace(/\D/g, "");
  if (digitos.startsWith("549") && digitos.length === 13) digitos = digitos.slice(3);
  else if (digitos.startsWith("54") && digitos.length === 12) digitos = digitos.slice(2);
  else if (digitos.startsWith("0") && digitos.length === 11) digitos = digitos.slice(1);
  return digitos.length === 10 && !digitos.startsWith("0") ? digitos : null;
}

/** Hasta dos iniciales para el avatar: "Automotores del Sur" → "AS". */
export function iniciales(nombre: string): string {
  const palabras = nombre
    .split(/\s+/)
    .filter((p) => p.length > 0 && !["de", "del", "la", "las", "los", "y", "e"].includes(p.toLowerCase()));
  const letras = (palabras.length > 1 ? [palabras[0], palabras[palabras.length - 1]] : palabras.slice(0, 1))
    .map((p) => p.charAt(0).toUpperCase())
    .join("");
  return letras || "?";
}

/** URL pública del perfil. */
export function urlDelPerfil(slug: string): string {
  return `${urlDelSitio()}/${slug}`;
}

/** URL del perfil sin protocolo, para mostrar: "devott.com/autos-del-sur". */
export function urlVisibleDelPerfil(slug: string): string {
  return urlDelPerfil(slug).replace(/^https?:\/\//, "").replace(/^www\./, "");
}
