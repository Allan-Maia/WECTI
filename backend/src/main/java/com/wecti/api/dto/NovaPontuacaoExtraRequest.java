package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Lancamento de pontos extras pelo admin (gincana, premiacao).
 *
 * @param periodoId opcional - sem ele, cai no periodo vigente, que e o
 *                  caso normal (o admin lanca durante o proprio evento)
 * @param pontos    pode ser negativo, para corrigir um lancamento a maior
 * @param motivo    obrigatorio de proposito: sem ele, ninguem consegue
 *                  explicar depois de onde vieram os pontos de um aluno
 *                  no ranking
 */
public record NovaPontuacaoExtraRequest(
        @NotNull(message = "Informe o aluno") UUID alunoId,
        UUID periodoId,
        @NotNull(message = "Informe a pontuacao") Integer pontos,
        @NotBlank(message = "Explique o motivo dos pontos")
        @Size(max = 200, message = "O motivo deve ter no maximo 200 caracteres")
        String motivo) {
}
