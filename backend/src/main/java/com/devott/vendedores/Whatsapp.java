package com.devott.vendedores;

import java.util.Optional;

/**
 * Números de WhatsApp argentinos. Se guardan como 549 + código de área + número (13 dígitos),
 * que es el formato que espera wa.me.
 */
final class Whatsapp {

    private Whatsapp() {
    }

    /**
     * Normaliza lo que carga el vendedor. Acepta el número con código de área (10 dígitos), con 0
     * adelante, o con el prefijo internacional 54 / 549. No acepta el 15: es ambiguo según el área.
     */
    static Optional<String> normalizar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.startsWith("549") && digitos.length() == 13) {
            digitos = digitos.substring(3);
        } else if (digitos.startsWith("54") && digitos.length() == 12) {
            digitos = digitos.substring(2);
        } else if (digitos.startsWith("0") && digitos.length() == 11) {
            digitos = digitos.substring(1);
        }
        return digitos.length() == 10 && !digitos.startsWith("0")
                ? Optional.of("549" + digitos)
                : Optional.empty();
    }

    /** Número local para mostrar en formularios: 5493534123456 → 3534123456. */
    static String local(String normalizado) {
        return normalizado.startsWith("549") ? normalizado.substring(3) : normalizado;
    }
}
