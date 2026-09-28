package com.devott.publicaciones;

import com.devott.cotizaciones.CotizacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Actualiza la cotización del dólar y recalcula el precio de referencia de las publicaciones en pesos.
 * Corre periódicamente y al arrancar la aplicación.
 */
@Component
class ActualizacionDePreciosUsd {

    private static final Logger log = LoggerFactory.getLogger(ActualizacionDePreciosUsd.class);

    private final CotizacionService cotizaciones;
    private final PublicacionService publicaciones;
    private final boolean alIniciar;

    ActualizacionDePreciosUsd(CotizacionService cotizaciones, PublicacionService publicaciones,
                              @Value("${devott.cotizaciones.al-iniciar}") boolean alIniciar) {
        this.cotizaciones = cotizaciones;
        this.publicaciones = publicaciones;
        this.alIniciar = alIniciar;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alArrancar() {
        if (alIniciar) {
            actualizar();
        }
    }

    @Scheduled(cron = "${devott.cotizaciones.cron}", zone = "America/Argentina/Buenos_Aires")
    void actualizar() {
        try {
            var cotizacion = cotizaciones.actualizar();
            log.info("Dólar {} del {}: {}", cotizacion.tipo(), cotizacion.fecha(), cotizacion.valor());
        } catch (RuntimeException e) {
            // Sin cotización nueva se recalcula con la última guardada.
            log.warn("No se pudo actualizar la cotización del dólar: {}", e.getMessage());
        }
        cotizaciones.dolarDeReferencia().ifPresent(dolar -> {
            int actualizadas = publicaciones.recalcularPreciosUsd(dolar);
            log.info("Precio en dólares recalculado en {} publicaciones en pesos", actualizadas);
        });
    }
}
