package com.wecti.api.dto;

import java.util.UUID;

public record EventoPontuacaoItemResponse(UUID eventoId, String titulo, int pontos, String status) {
}
