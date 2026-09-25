package com.devott.compartido.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados. `pagina` empieza en 0.
 */
@Schema(name = "Pagina")
public record Pagina<T>(List<T> items, int pagina, int tamano, long total, boolean hayMas) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapeo) {
        return new Pagina<>(page.getContent().stream().map(mapeo).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.hasNext());
    }

    public static <T> Pagina<T> de(Page<?> page, List<T> items) {
        return new Pagina<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    public <R> Pagina<R> map(Function<T, R> mapeo) {
        return new Pagina<>(items.stream().map(mapeo).toList(), pagina, tamano, total, hayMas);
    }
}
