import type { Metadata, Viewport } from "next";
import { Bricolage_Grotesque, Figtree } from "next/font/google";
import { Encabezado } from "@/components/layout/Encabezado";
import { urlDelSitio } from "@/lib/config";
import "./globals.css";

const figtree = Figtree({
  variable: "--font-figtree",
  subsets: ["latin"],
});

const bricolage = Bricolage_Grotesque({
  variable: "--font-bricolage",
  subsets: ["latin"],
  weight: ["600", "800"],
});

const descripcion =
  "Autos 0 km y usados de concesionarias y particulares cerca tuyo. Filtrá por zona y consultá por WhatsApp.";

export const metadata: Metadata = {
  metadataBase: new URL(urlDelSitio()),
  title: {
    default: "Devott · Autos en tu zona",
    template: "%s · Devott",
  },
  description: descripcion,
  openGraph: {
    siteName: "Devott",
    locale: "es_AR",
    type: "website",
    description: descripcion,
  },
};

export const viewport: Viewport = {
  themeColor: "#f3eee5",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es-AR" className={`${figtree.variable} ${bricolage.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        <Encabezado />
        {children}
      </body>
    </html>
  );
}
