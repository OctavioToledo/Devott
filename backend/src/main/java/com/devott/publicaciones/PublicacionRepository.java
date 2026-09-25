package com.devott.publicaciones;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    List<Publicacion> findByVendedorIdOrderByActualizadaEnDesc(UUID vendedorId);

    Optional<Publicacion> findByIdAndVendedorId(UUID id, UUID vendedorId);

    Optional<Publicacion> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByVendedorIdAndEstado(UUID vendedorId, EstadoPublicacion estado);

    Page<Publicacion> findByVendedorIdAndEstadoAndCondicionIn(UUID vendedorId, EstadoPublicacion estado,
                                                              Collection<Condicion> condiciones, Pageable pagina);
}
