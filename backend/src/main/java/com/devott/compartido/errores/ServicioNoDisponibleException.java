package com.devott.compartido.errores;

/**
 * Un servicio externo del que dependemos no respondió o respondió con error. Se responde con 503.
 */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
