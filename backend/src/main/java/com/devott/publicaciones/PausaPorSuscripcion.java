package com.devott.publicaciones;

import com.devott.suscripciones.SuscripcionTerminada;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Cuando un vendedor se queda sin plan, sus publicaciones activas pasan a pausadas (no se borra nada). */
@Component
class PausaPorSuscripcion {

    private final PublicacionRepository publicaciones;

    PausaPorSuscripcion(PublicacionRepository publicaciones) {
        this.publicaciones = publicaciones;
    }

    @EventListener
    void alTerminar(SuscripcionTerminada evento) {
        publicaciones.pausarActivas(evento.vendedorId());
    }
}
