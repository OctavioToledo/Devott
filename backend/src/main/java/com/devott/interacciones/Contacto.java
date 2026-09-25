package com.devott.interacciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Clic en un botón de WhatsApp. Cuenta para las métricas del vendedor y habilita reseñas (etapa 2).
 * `publicacionId` es null cuando el contacto viene del perfil del vendedor.
 */
@Entity
@Table(name = "contacto")
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "publicacion_id", updatable = false)
    private UUID publicacionId;

    @Column(name = "vendedor_id", nullable = false, updatable = false)
    private UUID vendedorId;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    protected Contacto() {
    }

    Contacto(UUID publicacionId, UUID vendedorId, UUID usuarioId) {
        this.publicacionId = publicacionId;
        this.vendedorId = vendedorId;
        this.usuarioId = usuarioId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public UUID getVendedorId() {
        return vendedorId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
