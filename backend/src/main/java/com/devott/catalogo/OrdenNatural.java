package com.devott.catalogo;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Ordena nombres comparando los números por su valor: "206" va antes que "2008" y "Tiggo 2" antes que "Tiggo 10".
 * El resto del texto se compara sin distinguir mayúsculas ni acentos, como en español.
 */
final class OrdenNatural implements Comparator<String> {

    static final OrdenNatural INSTANCIA = new OrdenNatural();

    private final Collator collator;

    private OrdenNatural() {
        collator = Collator.getInstance(Locale.forLanguageTag("es-AR"));
        collator.setStrength(Collator.PRIMARY);
    }

    @Override
    public int compare(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            int finA = finDelTramo(a, i);
            int finB = finDelTramo(b, j);
            String tramoA = a.substring(i, finA);
            String tramoB = b.substring(j, finB);

            int resultado = esNumero(tramoA) && esNumero(tramoB)
                    ? compararNumeros(tramoA, tramoB)
                    : collator.compare(tramoA, tramoB);
            if (resultado != 0) {
                return resultado;
            }
            i = finA;
            j = finB;
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    /** Un tramo es una racha de dígitos o una racha de no dígitos. */
    private static int finDelTramo(String texto, int inicio) {
        boolean digito = Character.isDigit(texto.charAt(inicio));
        int fin = inicio;
        while (fin < texto.length() && Character.isDigit(texto.charAt(fin)) == digito) {
            fin++;
        }
        return fin;
    }

    private static boolean esNumero(String tramo) {
        return Character.isDigit(tramo.charAt(0));
    }

    private static int compararNumeros(String a, String b) {
        String sinCerosA = a.replaceFirst("^0+(?=.)", "");
        String sinCerosB = b.replaceFirst("^0+(?=.)", "");
        int porLargo = Integer.compare(sinCerosA.length(), sinCerosB.length());
        return porLargo != 0 ? porLargo : sinCerosA.compareTo(sinCerosB);
    }
}
