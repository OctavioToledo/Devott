package com.devott.suscripciones;

/**
 * Límites del plan de un vendedor.
 *
 * @param maxPublicaciones publicaciones activas al mismo tiempo
 * @param maxFotos         fotos por publicación
 */
public record Limites(int maxPublicaciones, int maxFotos) {
}
