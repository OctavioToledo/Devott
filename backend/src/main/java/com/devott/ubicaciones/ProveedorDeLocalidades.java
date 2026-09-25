package com.devott.ubicaciones;

import java.util.List;

/**
 * Fuente de localidades. La implementación actual consulta la API Georef.
 */
public interface ProveedorDeLocalidades {

    /**
     * Busca localidades cuyo nombre contenga el texto. El texto ya viene normalizado.
     */
    List<Localidad> buscar(String texto);
}
