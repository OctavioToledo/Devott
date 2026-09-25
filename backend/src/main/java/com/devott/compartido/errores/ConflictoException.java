package com.devott.compartido.errores;

/**
 * La operación choca con el estado actual de los datos (por ejemplo, un slug ya usado). Se responde con 409.
 * Si se indica el campo, el error también se informa en "errores" para mostrarlo en el formulario.
 */
public class ConflictoException extends RuntimeException {

    private final String campo;

    public ConflictoException(String mensaje) {
        this(null, mensaje);
    }

    public ConflictoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
