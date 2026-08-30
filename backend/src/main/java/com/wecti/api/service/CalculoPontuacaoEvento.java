package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;

import java.time.LocalDateTime;

/**
 * Quanto um evento vale para um aluno. Regra unica, num lugar so.
 *
 * <p>Existe porque a mesma conta e feita em dois lugares que precisam
 * concordar: a pontuacao individual ({@link PontuacaoService}) e o
 * ranking ({@link RankingService}). Se cada um implementasse a regra, o
 * aluno veria um total na propria tela e outro no ranking - e o erro so
 * apareceria com a competicao ja em andamento.
 *
 * <p>A diferenca entre os dois nao esta na regra, e sim em como os dados
 * chegam: a tela individual busca inscricao e check-in por evento; o
 * ranking carrega tudo de uma vez e distribui. Por isso este metodo
 * recebe os objetos prontos, sem consultar nada.
 */
final class CalculoPontuacaoEvento {

    /** Aluno cumpriu o criterio e levou os pontos do evento. */
    static final String CONCLUIDO = "concluido";
    /** Nao cancelou e nao compareceu - perde os pontos do evento. */
    static final String NO_SHOW = "no_show";
    /** Cancelou no prazo - nao pontua nem penaliza. */
    static final String CANCELADO = "cancelado";

    private CalculoPontuacaoEvento() {
    }

    /**
     * @param inscricao inscricao do aluno no evento
     * @param checkin   check-in dele, ou {@code null} se nao houver
     * @return o resultado, ou {@code null} se o evento ainda nao deve
     *         entrar na conta (nao terminou)
     */
    static Resultado avaliar(Evento evento, Inscricao inscricao, Checkin checkin, LocalDateTime agora) {
        if (inscricao.getStatus() == InscricaoStatus.CANCELADA) {
            return new Resultado(0, CANCELADO);
        }

        // Evento em andamento ou futuro nao pontua nem penaliza: o aluno
        // ainda pode aparecer.
        if (evento.getDataHoraFim().isAfter(agora)) {
            return null;
        }

        if (checkin == null) {
            return new Resultado(-evento.getPontos(), NO_SHOW);
        }
        if (checkin.isPresencaQualificada(evento.getDataHoraInicio(), evento.getDataHoraFim())) {
            return new Resultado(evento.getPontos(), CONCLUIDO);
        }
        // Compareceu, mas nao ficou os 75% - nao ganha os pontos e
        // tambem nao e penalizado como quem faltou.
        return new Resultado(0, CONCLUIDO);
    }

    record Resultado(int pontos, String status) {
    }
}
