package com.devott.vendedores;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Usuarios de Instagram y Facebook. Se guarda solo el nombre de usuario, aunque se cargue la URL o con @.
 */
final class RedesSociales {

    private static final Pattern URL = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(instagram\\.com|facebook\\.com|fb\\.com)/", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTAGRAM = Pattern.compile("^[A-Za-z0-9._]{1,30}$");
    private static final Pattern FACEBOOK = Pattern.compile("^[A-Za-z0-9.\\-]{1,80}$");

    private RedesSociales() {
    }

    /** Devuelve vacío si no se cargó nada; lanza si se cargó algo inválido. */
    static Optional<String> instagram(String texto) {
        return usuario(texto, INSTAGRAM);
    }

    static Optional<String> facebook(String texto) {
        return usuario(texto, FACEBOOK);
    }

    private static Optional<String> usuario(String texto, Pattern valido) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String usuario = URL.matcher(texto.trim()).replaceFirst("")
                .replaceFirst("^@", "")
                .replaceFirst("[/?#].*$", "");
        if (!valido.matcher(usuario).matches()) {
            throw new IllegalArgumentException(usuario);
        }
        return Optional.of(usuario);
    }
}
