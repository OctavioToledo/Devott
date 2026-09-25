package com.devott.vendedores;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface VendedorRepository extends JpaRepository<Vendedor, UUID> {

    Optional<Vendedor> findByUsuarioId(UUID usuarioId);

    Optional<Vendedor> findBySlug(String slug);

    boolean existsByUsuarioId(UUID usuarioId);
}
