package com.devott.catalogo;

import com.devott.compartido.errores.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final MarcaRepository marcas;
    private final ModeloRepository modelos;

    CatalogoService(MarcaRepository marcas, ModeloRepository modelos) {
        this.marcas = marcas;
        this.modelos = modelos;
    }

    public List<Marca> marcas() {
        return marcas.findAll().stream()
                .sorted(Comparator.comparing(Marca::getNombre, OrdenNatural.INSTANCIA))
                .toList();
    }

    public List<Modelo> modelosDeMarca(Integer marcaId) {
        if (!marcas.existsById(marcaId)) {
            throw new RecursoNoEncontradoException("No existe la marca pedida.");
        }
        return modelos.findByMarcaId(marcaId).stream()
                .sorted(Comparator.comparing(Modelo::getNombre, OrdenNatural.INSTANCIA))
                .toList();
    }
}
