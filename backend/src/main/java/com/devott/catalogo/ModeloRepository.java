package com.devott.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ModeloRepository extends JpaRepository<Modelo, Integer> {

    List<Modelo> findByMarcaId(Integer marcaId);
}
