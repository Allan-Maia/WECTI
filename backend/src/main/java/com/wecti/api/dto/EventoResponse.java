package com.wecti.api.dto;

import com.wecti.api.domain.Evento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @param capacidade      vagas totais, ou {@code null} para evento sem limite
 * @param inscritos       vagas ocupadas agora (so inscricoes ativas)
 * @param vagasRestantes    {@code null} quando nao ha limite. Vem calculado
 *                          da API de proposito: se cada tela fizesse a
 *                          propria subtracao, uma delas acabaria esquecendo
 *                          do caso "sem limite" e mostrando vaga negativa.
 * @param inscricoesAbertas se ainda da para se inscrever (ou cancelar).
 *                          Tambem vem pronto da API pelo mesmo motivo: a
 *                          regra e "ate o inicio do evento, mais uma folga
 *                          configuravel", e uma tela que recalculasse isso
 *                          por conta ficaria fora de sincronia com o
 *                          backend - a regra ja mudou duas vezes.
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
        boolean inscricoesAbertas,
        LocalDateTime inscricoesAte,
        boolean emAndamento,
        boolean encerrado,
        List<PalestranteResponse> palestrantes) {

    /**
     * @param inscricoesAte instante em que as inscricoes fecham - vem de
     *                      PrazoInscricao, nao e calculado aqui: o DTO
     *                      transporta a decisao, nao a toma.
     */
    public static EventoResponse de(Evento evento, long inscritos,
                                     boolean inscricoesAbertas, LocalDateTime inscricoesAte) {
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
                inscricoesAbertas,
                inscricoesAte,
                evento.isEmAndamento(),
                evento.isEncerrado(),
                evento.getPalestrantes().stream().map(PalestranteResponse::de).toList());
    }
}
