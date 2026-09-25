package com.devott.compartido.errores;

/**
 * Un dato de la solicitud no cumple una regla que no se puede expresar con Bean Validation
 * (por ejemplo, un WhatsApp que no se puede normalizar). Se responde con 400 y el error en el campo.
 */
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
