package com.wecti.api.exception;

/**
 * Violacao de regra de negocio (ex.: prazo de cancelamento vencido,
 * criterios de certificado nao cumpridos). Mapeada para 422.
 */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
