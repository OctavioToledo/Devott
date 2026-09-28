import type { Metadata } from "next";
import { clasesBoton } from "@/components/ui/boton";
import { pedirApi } from "@/lib/api/cliente";
import type { Plan } from "@/lib/api/tipos";
import { usuarioActual } from "@/lib/auth/sesion";
import { whatsappDeVentas } from "@/lib/config";
import { linkParaPedirPlan, precioMensual } from "@/lib/planes/formato";
import { miVendedor } from "@/lib/vendedores/consultas";
import { urlDelPerfil } from "@/lib/vendedores/formato";

export const metadata: Metadata = {
  title: "Planes para vender tu auto",
  description: "Publicá tu stock en Devott con tu propio link para compartir. Planes para particulares y concesionarias.",
  alternates: { canonical: "/planes" },
};

const INCLUIDO = [
  "Perfil público con tu link propio",
  "Botón de WhatsApp en cada auto",
  "Métricas de visitas, clics y guardados",
];

export default async function Planes() {
  const [planes, usuario] = await Promise.all([
    pedirApi<Plan[]>("/planes", { revalidar: 300 }),
    usuarioActual(),
  ]);
  const vendedor = usuario ? await miVendedor().catch(() => null) : null;
  const ventas = whatsappDeVentas();
  const destacado = "CONCESIONARIA";

  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-4 pt-6 pb-16">
      <div className="flex max-w-2xl flex-col gap-2">
        <h1 className="font-titulo text-[34px] leading-[1.02] font-extrabold tracking-[-1.2px] sm:text-5xl">
          Publicá tu stock y compartilo con un link.
        </h1>
        <p className="text-[17px] text-secundario">
          Precios mensuales, sin comisiones por venta. Los compradores te escriben directo a tu WhatsApp.
        </p>
      </div>

      <ul className="grid gap-4 md:grid-cols-3">
        {planes.map((plan) => {
          const esDestacado = plan.codigo === destacado;
          return (
            <li
              key={plan.codigo}
              className={`flex flex-col gap-4 rounded-[22px] border bg-superficie p-5 ${
                esDestacado ? "border-tinta ring-1 ring-tinta" : "border-borde"
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <h2 className="font-titulo text-2xl font-extrabold tracking-[-0.5px]">{plan.nombre}</h2>
                {esDestacado && (
                  <span className="rounded-full bg-tinta px-2.5 py-1 text-[11px] font-bold tracking-[0.4px] text-fondo">
                    MÁS ELEGIDO
                  </span>
                )}
              </div>
              <p>
                <span className="font-titulo text-[34px] leading-none font-extrabold tracking-[-1px]">{precioMensual(plan)}</span>
                <span className="text-secundario"> /mes</span>
              </p>
              <ul className="flex flex-col gap-2 text-[15px]">
                <Item>
                  Hasta <strong>{plan.maxPublicaciones}</strong> autos publicados
                </Item>
                <Item>
                  <strong>{plan.maxFotos}</strong> fotos por auto
                </Item>
                {INCLUIDO.map((i) => (
                  <Item key={i}>{i}</Item>
                ))}
              </ul>
              {ventas && (
                <a
                  href={linkParaPedirPlan(ventas, plan, vendedor ? urlDelPerfil(vendedor.slug) : undefined)}
                  rel="noopener"
                  target="_blank"
                  className={clasesBoton(esDestacado ? "marca" : "oscuro", "lg", "mt-auto font-bold")}
                >
                  Quiero este plan
                </a>
              )}
            </li>
          );
        })}
      </ul>

      <section className="flex flex-col gap-1.5 rounded-[22px] bg-celeste p-5 text-confianza-profundo">
        <h2 className="font-bold">¿Tenés una concesionaria?</h2>
        <p className="text-[15px]">
          Consultanos por la prueba gratis: te damos un período para que cargues tu stock y veas cómo funciona antes de pagar.
        </p>
      </section>

      <p className="text-sm text-secundario">
        Se paga por transferencia o Mercado Pago y lo activamos a mano en el día. Si no se renueva, tenés 7 días de
        gracia; después tus autos se pausan (no se borra nada) hasta que lo renueves.
      </p>
    </main>
  );
}

function Item({ children }: { children: React.ReactNode }) {
  return (
    <li className="flex items-start gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="mt-0.5 shrink-0 text-confianza">
        <path d="M5 12.5l4.5 4.5L19 7.5" />
      </svg>
      <span>{children}</span>
    </li>
  );
}
