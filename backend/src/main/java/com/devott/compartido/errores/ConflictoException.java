package com.devott.compartido.errores;

/**
 * La operación choca con el estado actual de los datos (por ejemplo, un slug ya usado). Se responde con 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
