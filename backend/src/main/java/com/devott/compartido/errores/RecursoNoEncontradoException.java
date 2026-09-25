package com.devott.compartido.errores;

/**
 * El recurso pedido no existe o el usuario no puede verlo. Se responde con 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
