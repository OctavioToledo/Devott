package com.devott.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Marca de vehículos. El catálogo se carga con la migración R__catalogo.sql y es de solo lectura.
 */
@Entity
@Immutable
@Table(name = "marca")
public class Marca {

    @Id
    private Integer id;

    private String nombre;

    private String slug;

    protected Marca() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }
}
