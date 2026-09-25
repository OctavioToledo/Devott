package com.devott.ubicaciones;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UbicacionService {

    static final int MAXIMO_RESULTADOS = 10;

    private final ProveedorDeLocalidades proveedor;

    UbicacionService(ProveedorDeLocalidades proveedor) {
        this.proveedor = proveedor;
    }

    /**
     * Busca localidades por nombre. Normaliza el texto para aprovechar la caché ("Villa María " y
     * "villa maria" son la misma búsqueda) y quita las repetidas (mismo nombre en la misma provincia).
     */
    public List<Localidad> buscar(String texto) {
        Map<String, Localidad> unicas = new LinkedHashMap<>();
        for (Localidad localidad : proveedor.buscar(normalizar(texto))) {
            unicas.putIfAbsent(normalizar(localidad.nombre()) + "|" + localidad.provincia(), localidad);
        }
        return unicas.values().stream().limit(MAXIMO_RESULTADOS).toList();
    }

    static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
