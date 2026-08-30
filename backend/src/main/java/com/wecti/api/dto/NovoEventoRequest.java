package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Sem periodoId aqui de proposito: o admin so escolhe data/hora do evento
// (ja e o suficiente pra saber quando ele acontece) - o Periodo (semestre,
// usado pra resetar a pontuacao - ver CLAUDE.md) e descoberto sozinho pelo
// EventoService a partir dessa data, comparando com o intervalo
// data_inicio/data_fim de cada Periodo ja cadastrado.
public record NovoEventoRequest(
        @NotBlank String titulo,
        String descricao,
        String local,
        @NotNull LocalDateTime dataHoraInicio,
        @NotNull LocalDateTime dataHoraFim,
        @NotNull @PositiveOrZero Integer pontos,
        /**
         * Numero de vagas. Opcional: deixar em branco (null) significa
         * sem limite, que e como todos os eventos funcionavam antes -
         * nao da para exigir aqui sem invalidar o que ja existe.
         */
        @Positive(message = "A capacidade deve ser de pelo menos 1 vaga") Integer capacidade,
        List<UUID> palestranteIds) {
}
