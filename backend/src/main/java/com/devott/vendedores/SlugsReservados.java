package com.devott.vendedores;

import java.util.Set;

/**
 * Slugs que no puede usar un vendedor porque chocan con rutas del sitio (devott.com/ingresar)
 * o se prestan a confusión. Si se agrega una ruta de primer nivel en el frontend, sumarla acá.
 */
final class SlugsReservados {

    static final Set<String> RESERVADOS = Set.of(
            "admin", "api", "auth", "autos", "ayuda", "blog", "buscar", "contacto", "cuenta", "devott",
            "explorar", "favoritos", "guardados", "ingresar", "login", "logout", "mi-cuenta", "panel",
            "planes", "precios", "privacidad", "publicaciones", "publicacion", "publicar", "registro",
            "salir", "seguidos", "sitemap", "soporte", "terminos", "vendedores", "vendedor", "www");

    private SlugsReservados() {
    }

    static boolean esReservado(String slug) {
        return RESERVADOS.contains(slug);
    }
}
