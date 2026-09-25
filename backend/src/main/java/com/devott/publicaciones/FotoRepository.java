package com.devott.publicaciones;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface FotoRepository extends JpaRepository<Foto, UUID> {

    List<Foto> findByPublicacionIdOrderByOrdenAsc(UUID publicacionId);

    List<Foto> findByPublicacionIdInOrderByOrdenAsc(Collection<UUID> publicacionIds);

    long countByPublicacionId(UUID publicacionId);
}
