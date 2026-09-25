/** Silueta de auto para cuando la publicación todavía no tiene fotos. */
export function SiluetaAuto({ ancho = 120 }: { ancho?: number }) {
  return (
    <svg width={ancho} height={(ancho * 46) / 110} viewBox="0 0 110 46" fill="none" stroke="#17150F" strokeOpacity="0.4" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true">
      <path d="M6 36 L6 29 C7.5 24.5 12 22.5 19 21.5 L33 20 L44 11.5 C46.5 9.5 49.5 9 53 9 L73 9 C78 9 81.5 10.5 84.5 14 L89.5 19.5 C97.5 20.5 103 23 104 29 L104 36" />
      <path d="M6 36 H20 M36 36 H84 M100 36 H104" />
      <circle cx="28" cy="36" r="8" />
      <circle cx="92" cy="36" r="8" />
    </svg>
  );
}
