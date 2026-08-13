package com.wecti.api.dto;

import java.util.List;
import java.util.UUID;

public record PontuacaoPeriodoResponse(
        UUID periodoId,
        String periodoNome,
        int pontosTotal,
        List<EventoPontuacaoItemResponse> eventos) {
}
