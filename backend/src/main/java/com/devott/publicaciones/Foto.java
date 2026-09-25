package com.devott.publicaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Foto de una publicación. El archivo vive en el almacenamiento (Supabase Storage); acá se guarda
 * su ruta y el orden. La de orden 0 es la portada.
 */
@Entity
@Table(name = "foto")
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "publicacion_id", nullable = false, updatable = false)
    private UUID publicacionId;

    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    private int orden;

    private Integer ancho;

    private Integer alto;

    protected Foto() {
    }

    Foto(UUID publicacionId, String storagePath, int orden, Integer ancho, Integer alto) {
        this.publicacionId = publicacionId;
        this.storagePath = storagePath;
        this.orden = orden;
        this.ancho = ancho;
        this.alto = alto;
    }

    void ordenar(int orden) {
        this.orden = orden;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public int getOrden() {
        return orden;
    }

    public Integer getAncho() {
        return ancho;
    }

    public Integer getAlto() {
        return alto;
    }
}
