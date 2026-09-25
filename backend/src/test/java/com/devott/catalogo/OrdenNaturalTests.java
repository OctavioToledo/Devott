package com.devott.catalogo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrdenNaturalTests {

    private static List<String> ordenar(String... nombres) {
        return List.of(nombres).stream().sorted(OrdenNatural.INSTANCIA).toList();
    }

    @Test
    void ordenaLosNumerosPorSuValor() {
        assertThat(ordenar("2008", "208", "3008", "206", "301"))
                .containsExactly("206", "208", "301", "2008", "3008");
    }

    @Test
    void ordenaNumerosDentroDelNombre() {
        assertThat(ordenar("Tiggo 8", "Tiggo 10", "Tiggo 2"))
                .containsExactly("Tiggo 2", "Tiggo 8", "Tiggo 10");
    }

    @Test
    void ponePrimeroLosNumerosYDespuesElTexto() {
        assertThat(ordenar("Partner", "208", "Boxer"))
                .containsExactly("208", "Boxer", "Partner");
    }

    @Test
    void ignoraMayusculasYAcentos() {
        assertThat(ordenar("Scénic", "sandero", "Mégane", "Master"))
                .containsExactly("Master", "Mégane", "sandero", "Scénic");
    }

    @Test
    void elNombreMasCortoVaPrimeroSiEsPrefijo() {
        assertThat(ordenar("Onix Plus", "Onix"))
                .containsExactly("Onix", "Onix Plus");
    }
}
