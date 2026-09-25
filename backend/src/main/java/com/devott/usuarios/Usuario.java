package com.devott.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Usuario de Devott. El id es el mismo que en Supabase Auth.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    private String email;

    private String nombre;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private Instant creadoEn;

    protected Usuario() {
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
