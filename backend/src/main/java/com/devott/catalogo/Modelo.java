package com.devott.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Modelo de una marca. De solo lectura, igual que las marcas.
 */
@Entity
@Immutable
@Table(name = "modelo")
public class Modelo {

    @Id
    private Integer id;

    @Column(name = "marca_id")
    private Integer marcaId;

    private String nombre;

    private String slug;

    protected Modelo() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getMarcaId() {
        return marcaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }
}
