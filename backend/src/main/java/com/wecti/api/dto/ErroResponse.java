package com.wecti.api.dto;

import java.time.Instant;

public record ErroResponse(Instant timestamp, int status, String erro, String mensagem, String campo) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, null);
    }

    public static ErroResponse de(int status, String erro, String mensagem, String campo) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, campo);
    }
}
