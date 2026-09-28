package com.devott.suscripciones;

import java.util.UUID;

/**
 * Evento: el vendedor se quedó sin plan (venció la gracia o se canceló). Publicaciones lo escucha
 * para pausar sus publicaciones activas; se publica dentro de la transacción que la termina.
 */
public record SuscripcionTerminada(UUID vendedorId) {
}
