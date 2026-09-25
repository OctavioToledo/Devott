package com.devott.publicaciones;

import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida de una publicación. Solo las ACTIVA aparecen en el feed y en el perfil;
 * las VENDIDA se siguen viendo por su link, con el cartel de vendido.
 */
public enum EstadoPublicacion {
    BORRADOR,
    ACTIVA,
    PAUSADA,
    VENDIDA;

    private static final Map<EstadoPublicacion, Set<EstadoPublicacion>> TRANSICIONES = Map.of(
            BORRADOR, Set.of(ACTIVA),
            ACTIVA, Set.of(PAUSADA, VENDIDA),
            PAUSADA, Set.of(ACTIVA, VENDIDA),
            // Se permite volver a activar una vendida por si se marcó por error.
            VENDIDA, Set.of(ACTIVA));

    public boolean puedePasarA(EstadoPublicacion nuevo) {
        return TRANSICIONES.get(this).contains(nuevo);
    }

    public boolean esVisibleEnPublico() {
        return this == ACTIVA || this == VENDIDA;
    }
}
