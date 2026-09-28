package com.devott.suscripciones;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Límites de publicaciones y fotos de cada vendedor, según su plan vigente (con los días de gracia).
 * Sin plan no puede tener publicaciones activas, pero sí preparar borradores con fotos.
 */
@Service
public class LimitesService {

    /** Sin plan: borradores con tantas fotos como el plan más chico. */
    public static final Limites SIN_PLAN = new Limites(0, 8);

    private final SuscripcionService suscripciones;

    LimitesService(SuscripcionService suscripciones) {
        this.suscripciones = suscripciones;
    }

    public Limites de(UUID vendedorId) {
        return suscripciones.vigente(vendedorId).map(s -> s.plan().limites()).orElse(SIN_PLAN);
    }
}
