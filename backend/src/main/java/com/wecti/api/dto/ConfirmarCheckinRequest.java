package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Codigo rotativo lido do QR que estava na tela (parametro {@code c} da
 * URL). Vai no corpo, e nao na query string, pra nao ficar registrado nos
 * logs de acesso do servidor web - de la alguem poderia pescar codigos
 * ainda dentro da janela.
 */
public record ConfirmarCheckinRequest(
        @NotBlank(message = "QR code invalido - escaneie o que esta na tela")
        String codigo) {
}
