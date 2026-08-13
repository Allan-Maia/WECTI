package com.wecti.api.dto;

import com.wecti.api.domain.Evento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String titulo,
        String descricao,
        UUID periodoId,
        String local,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer pontos,
        List<PalestranteResponse> palestrantes) {

    public static EventoResponse de(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescricao(),
                evento.getPeriodo().getId(),
                evento.getLocal(),
                evento.getDataHoraInicio(),
                evento.getDataHoraFim(),
                evento.getPontos(),
                evento.getPalestrantes().stream().map(PalestranteResponse::de).toList());
    }
}
