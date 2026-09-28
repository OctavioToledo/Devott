package com.devott.publicaciones;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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

    /** No toca actualizada_en: el cambio de cotización no es una edición del vendedor. */
    @Modifying
    @Query(value = """
            UPDATE publicacion SET precio_usd_ref = round(precio / :dolar, 2)
            WHERE moneda = 'ARS' AND precio_usd_ref IS DISTINCT FROM round(precio / :dolar, 2)
            """, nativeQuery = true)
    int recalcularPreciosUsd(BigDecimal dolar);

    /** Pausa todas las activas del vendedor (se quedó sin plan). */
    @Modifying
    @Query(value = "UPDATE publicacion SET estado = 'PAUSADA', actualizada_en = now() WHERE vendedor_id = :vendedorId AND estado = 'ACTIVA'",
            nativeQuery = true)
    int pausarActivas(UUID vendedorId);
}
