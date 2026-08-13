package com.wecti.api.dto;

import com.wecti.api.domain.Periodo;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodoResponse(UUID id, String nome, LocalDate dataInicio, LocalDate dataFim) {

    public static PeriodoResponse de(Periodo periodo) {
        return new PeriodoResponse(periodo.getId(), periodo.getNome(), periodo.getDataInicio(), periodo.getDataFim());
    }
}
