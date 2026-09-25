import { Encabezado } from "@/components/layout/Encabezado";

export default function LayoutSitio({ children }: LayoutProps<"/">) {
  return (
    <>
      <Encabezado />
      {children}
    </>
  );
}
