package com.americana.bibliotecaserver.exception;

public class ConflictoNegocioException extends RuntimeException {
    private final String codigo;
    private final String campo;

    public ConflictoNegocioException(String codigo, String campo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.campo = campo;
    }

    public String getCodigo() { return codigo; }
    public String getCampo() { return campo; }
}
