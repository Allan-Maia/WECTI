package com.wecti.api.exception;

/**
 * Erro de validacao de um campo especifico que nao pode ser expresso via
 * bean validation (ex.: RGM obrigatorio apenas quando perfil = ALUNO).
 * Mapeada para 400.
 */
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
