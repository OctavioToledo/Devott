package com.devott.publicaciones;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/**
 * 0 km o usado. En la base y en la API el valor es "0KM", que no es un nombre válido de constante.
 */
public enum Condicion {
    CERO_KM("0KM"),
    USADO("USADO");

    private final String codigo;

    Condicion(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String codigo() {
        return codigo;
    }

    @JsonCreator
    public static Condicion deCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(c -> c.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Condición inválida: " + codigo));
    }

    @Converter(autoApply = true)
    static class ConversorJpa implements AttributeConverter<Condicion, String> {

        @Override
        public String convertToDatabaseColumn(Condicion condicion) {
            return condicion == null ? null : condicion.codigo;
        }

        @Override
        public Condicion convertToEntityAttribute(String codigo) {
            return codigo == null ? null : deCodigo(codigo);
        }
    }
}
