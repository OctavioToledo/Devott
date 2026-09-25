package com.devott.interacciones;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ContactoRepository extends JpaRepository<Contacto, UUID> {

    List<Contacto> findByVendedorId(UUID vendedorId);
}
