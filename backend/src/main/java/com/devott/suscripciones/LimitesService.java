package com.devott.suscripciones;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Límites de publicaciones y fotos de cada vendedor. Por ahora todos tienen los mismos,
 * definidos en la configuración; en el paso 10 salen de su suscripción.
 */
@Service
public class LimitesService {

    private final Limites porDefecto;

    LimitesService(@Value("${devott.limites.max-publicaciones}") int maxPublicaciones,
                   @Value("${devott.limites.max-fotos}") int maxFotos) {
        this.porDefecto = new Limites(maxPublicaciones, maxFotos);
    }

    public Limites de(UUID vendedorId) {
        return porDefecto;
    }
}
