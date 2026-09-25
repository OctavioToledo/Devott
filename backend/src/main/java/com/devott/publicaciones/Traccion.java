package com.devott.publicaciones;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * Tracción. En la base y en la API el valor de 4x4 es "4X4", que no es un nombre válido de constante.
 */
public enum Traccion {
    DELANTERA("DELANTERA"),
    TRASERA("TRASERA"),
    CUATRO_X_CUATRO("4X4"),
    AWD("AWD");

    private final String codigo;

    Traccion(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String codigo() {
        return codigo;
    }

    @JsonCreator
    public static Traccion deCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(t -> t.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tracción inválida: " + codigo));
    }

    @Converter(autoApply = true)
    static class ConversorJpa implements AttributeConverter<Traccion, String> {

        @Override
        public String convertToDatabaseColumn(Traccion traccion) {
            return traccion == null ? null : traccion.codigo;
        }

        @Override
        public Traccion convertToEntityAttribute(String codigo) {
            return codigo == null ? null : deCodigo(codigo);
        }
    }
}
