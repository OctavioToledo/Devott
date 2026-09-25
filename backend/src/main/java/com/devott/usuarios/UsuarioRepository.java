package com.devott.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Crea el usuario o actualiza sus datos en una sola sentencia, así dos requests simultáneos
     * del mismo usuario recién registrado no chocan. Los datos nulos no pisan los existentes.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO usuario (id, email, nombre, avatar_url)
            VALUES (:id, :email, :nombre, :avatarUrl)
            ON CONFLICT (id) DO UPDATE SET
                email = COALESCE(EXCLUDED.email, usuario.email),
                nombre = COALESCE(EXCLUDED.nombre, usuario.nombre),
                avatar_url = COALESCE(EXCLUDED.avatar_url, usuario.avatar_url)
            """, nativeQuery = true)
    void upsert(UUID id, String email, String nombre, String avatarUrl);
}
