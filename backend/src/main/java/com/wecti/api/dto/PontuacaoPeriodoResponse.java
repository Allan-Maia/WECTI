package com.wecti.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * @param pontosTotal   o que vale no ranking: eventos + extras
 * @param pontosEventos parcela vinda das palestras assistidas
 * @param pontosExtras  parcela lancada pelo admin (gincanas)
 * @param extras        lancamentos com motivo e autor - sem isso o aluno
 *                      ve o total mudar e nao tem como saber por que
 */
public record PontuacaoPeriodoResponse(
        UUID periodoId,
        String periodoNome,
        int pontosTotal,
        int pontosEventos,
        int pontosExtras,
        List<EventoPontuacaoItemResponse> eventos,
        List<PontuacaoExtraResponse> extras) {
}
