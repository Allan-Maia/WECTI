package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NovoEventoRequest(
        @NotBlank String titulo,
        String descricao,
        @NotNull UUID periodoId,
        String local,
        @NotNull LocalDateTime dataHoraInicio,
        @NotNull LocalDateTime dataHoraFim,
        @NotNull @PositiveOrZero Integer pontos,
        List<UUID> palestranteIds) {
}
