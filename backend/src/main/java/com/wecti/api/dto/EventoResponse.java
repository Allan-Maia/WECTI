package com.wecti.api.dto;

import com.wecti.api.domain.Evento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @param capacidade      vagas totais, ou {@code null} para evento sem limite
 * @param inscritos       vagas ocupadas agora (so inscricoes ativas)
 * @param vagasRestantes  {@code null} quando nao ha limite. Vem calculado
 *                        da API de proposito: se cada tela fizesse a
 *                        propria subtracao, uma delas acabaria esquecendo
 *                        do caso "sem limite" e mostrando vaga negativa.
 */
public record EventoResponse(
        UUID id,
        String titulo,
        String descricao,
        String local,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer pontos,
        Integer capacidade,
        long inscritos,
        Integer vagasRestantes,
        boolean lotado,
        List<PalestranteResponse> palestrantes) {

    public static EventoResponse de(Evento evento, long inscritos) {
        Integer capacidade = evento.getCapacidade();
        Integer restantes = capacidade == null ? null : (int) Math.max(0, capacidade - inscritos);
        return new EventoResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescricao(),
                evento.getLocal(),
                evento.getDataHoraInicio(),
                evento.getDataHoraFim(),
                evento.getPontos(),
                capacidade,
                inscritos,
                restantes,
                restantes != null && restantes == 0,
                evento.getPalestrantes().stream().map(PalestranteResponse::de).toList());
    }
}
