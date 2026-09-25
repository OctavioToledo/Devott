package com.devott.catalogo;

import com.devott.compartido.errores.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public Optional<ModeloConMarca> modeloConMarca(Integer modeloId) {
        return modelosConMarca(List.of(modeloId)).values().stream().findFirst();
    }

    /** Modelos con su marca, por id. Los ids que no existen no aparecen en el resultado. */
    public Map<Integer, ModeloConMarca> modelosConMarca(Collection<Integer> modeloIds) {
        List<Modelo> encontrados = modelos.findAllById(modeloIds);
        Map<Integer, Marca> marcasPorId = marcas.findAllById(encontrados.stream().map(Modelo::getMarcaId).toList())
                .stream().collect(Collectors.toMap(Marca::getId, Function.identity()));
        return encontrados.stream().collect(Collectors.toMap(Modelo::getId, m -> {
            Marca marca = marcasPorId.get(m.getMarcaId());
            return new ModeloConMarca(m.getId(), m.getNombre(), m.getSlug(),
                    marca.getId(), marca.getNombre(), marca.getSlug());
        }));
    }
}
