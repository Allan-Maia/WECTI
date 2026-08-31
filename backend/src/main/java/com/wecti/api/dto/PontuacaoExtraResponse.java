package com.wecti.api.dto;

import com.wecti.api.domain.PontuacaoExtra;

import java.time.LocalDateTime;
import java.util.UUID;

public record PontuacaoExtraResponse(
        UUID id,
        UUID alunoId,
        String alunoNome,
        int pontos,
        String motivo,
        String criadoPorNome,
        LocalDateTime criadoEm) {

    public static PontuacaoExtraResponse de(PontuacaoExtra extra) {
        return new PontuacaoExtraResponse(
                extra.getId(),
                extra.getAluno().getId(),
                extra.getAluno().getNome(),
                extra.getPontos(),
                extra.getMotivo(),
                extra.getCriadoPor().getNome(),
                extra.getCriadoEm());
    }
}
