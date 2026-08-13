package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record NovoPeriodoRequest(
        @NotBlank String nome,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim) {
}
