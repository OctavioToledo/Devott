package com.devott.suscripciones;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "Plan")
public record Plan(String codigo, String nombre, int maxPublicaciones, int maxFotos, BigDecimal precioArs) {

    public Limites limites() {
        return new Limites(maxPublicaciones, maxFotos);
    }
}
