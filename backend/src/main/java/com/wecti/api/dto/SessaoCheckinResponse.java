package com.wecti.api.dto;

import com.wecti.api.domain.SessaoCheckin;
import com.wecti.api.domain.TipoSessaoCheckin;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessaoCheckinResponse(
        UUID id,
        UUID eventoId,
        TipoSessaoCheckin tipo,
        LocalDateTime criadaEm,
        LocalDateTime expiraEm) {

    public static SessaoCheckinResponse de(SessaoCheckin sessao) {
        return new SessaoCheckinResponse(sessao.getId(), sessao.getEvento().getId(), sessao.getTipo(),
                sessao.getCriadaEm(), sessao.getExpiraEm());
    }
}
