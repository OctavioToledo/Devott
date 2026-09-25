package com.devott.compartido.texto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Slugs para URLs: minúsculas, sin acentos, palabras separadas por guiones.
 */
public final class Slugs {

    public static final String PATRON = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private static final Pattern VALIDO = Pattern.compile(PATRON);

    private Slugs() {
    }

    /** Genera un slug a partir de un texto libre: "Automotores del Sur" → "automotores-del-sur". */
    public static String generar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    public static boolean esValido(String slug) {
        return slug != null && VALIDO.matcher(slug).matches();
    }
}
