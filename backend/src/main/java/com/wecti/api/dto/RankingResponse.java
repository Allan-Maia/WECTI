package com.wecti.api.dto;

import java.util.List;
import java.util.UUID;

public record RankingResponse(
        UUID periodoId,
        String periodoNome,
        List<RankingItemResponse> itens) {
}
