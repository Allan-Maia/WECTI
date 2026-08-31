package com.wecti.api.dto;

import java.util.List;

/**
 * Pontuacao do aluno no WECTI.
 *
 * <p>Sem recorte por semestre: o conceito de "periodo" saiu do sistema
 * (ver V7__remove_periodo.sql). O total e simplesmente o que o aluno
 * somou.
 *
 * @param pontosTotal   o que vale no ranking: eventos + extras
 * @param pontosEventos parcela vinda das palestras assistidas
 * @param pontosExtras  parcela lancada pelo admin (gincanas)
 * @param extras        lancamentos com motivo e autor - sem isso o aluno
 *                      ve o total mudar e nao tem como saber por que
 */
public record PontuacaoResponse(
        int pontosTotal,
        int pontosEventos,
        int pontosExtras,
        List<EventoPontuacaoItemResponse> eventos,
        List<PontuacaoExtraResponse> extras) {
}
