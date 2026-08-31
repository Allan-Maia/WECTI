package com.wecti.api.dto;

import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InscricaoResponse(
        UUID id,
        UUID alunoId,
        UUID eventoId,
        String status,
        LocalDateTime criadaEm,
        LocalDateTime canceladaEm,
        CheckinResponse checkin,
        boolean certificadoDisponivel) {

    public static InscricaoResponse de(Inscricao inscricao, CheckinResponse checkin, boolean certificadoDisponivel) {
        return new InscricaoResponse(
                inscricao.getId(),
                inscricao.getAluno().getId(),
                inscricao.getEvento().getId(),
                inscricao.getStatus() == InscricaoStatus.ATIVA ? "ativa" : "cancelada",
                inscricao.getCriadaEm(),
                inscricao.getCanceladaEm(),
                checkin,
                certificadoDisponivel);
    }
}
