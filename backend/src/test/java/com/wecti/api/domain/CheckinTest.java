package com.wecti.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regra de presenca qualificada: check-out registrado + permanencia >= 75%
 * da duracao do evento. E o criterio que decide TANTO o certificado quanto
 * a pontuacao, entao errar aqui reprova aluno que compareceu ou premia
 * quem nao ficou.
 */
class CheckinTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 20, 19, 0);

    private Checkin checkin(LocalDateTime entrada, LocalDateTime saida) {
        return Checkin.builder().entrada(entrada).saida(saida).build();
    }

    @Nested
    @DisplayName("permanencia suficiente")
    class Aprovado {

        @Test
        @DisplayName("fica o evento inteiro")
        void eventoInteiro() {
            LocalDateTime fim = INICIO.plusHours(2);
            assertThat(checkin(INICIO, fim).isPresencaQualificada(INICIO, fim)).isTrue();
        }

        @Test
        @DisplayName("fica exatamente 75% - o limite conta como aprovado")
        void exatamenteNoLimite() {
            LocalDateTime fim = INICIO.plusHours(4);
            LocalDateTime saida = INICIO.plusHours(3); // 3h de 4h = 75%
            assertThat(checkin(INICIO, saida).isPresencaQualificada(INICIO, fim)).isTrue();
        }

        @Test
        @DisplayName("chega atrasado mas ainda cumpre os 75%")
        void chegaAtrasado() {
            LocalDateTime fim = INICIO.plusHours(4);
            LocalDateTime entrada = INICIO.plusMinutes(50);
            assertThat(checkin(entrada, fim).isPresencaQualificada(INICIO, fim)).isTrue();
        }

        /**
         * Este e o caso que JA QUEBROU em producao. O calculo usava
         * Duration.toMinutes(), que trunca a fracao: 3min53s virava "3
         * minutos", e 3 de 5 da 60% - reprovando um aluno que na verdade
         * ficou 77,7% do evento. Em evento curto o erro e enorme; por isso
         * a conta e feita em segundos.
         */
        @Test
        @DisplayName("evento curto: 3min53s de 5min = 77,7% (regressao do bug de truncamento)")
        void eventoCurtoNaoPodeTruncarMinutos() {
            LocalDateTime fim = INICIO.plusMinutes(5);
            LocalDateTime entrada = INICIO.plusMinutes(1).plusSeconds(20);
            LocalDateTime saida = INICIO.plusMinutes(5).plusSeconds(13);

            assertThat(checkin(entrada, saida).isPresencaQualificada(INICIO, fim))
                    .as("233s de 300s = 77,7%%, acima dos 75%% exigidos")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("permanencia insuficiente")
    class Reprovado {

        @Test
        @DisplayName("sem check-out nao qualifica, mesmo tendo entrado no inicio")
        void semCheckout() {
            LocalDateTime fim = INICIO.plusHours(2);
            assertThat(checkin(INICIO, null).isPresencaQualificada(INICIO, fim)).isFalse();
        }

        @Test
        @DisplayName("sai logo depois de entrar")
        void saiCedoDemais() {
            LocalDateTime fim = INICIO.plusHours(2);
            LocalDateTime saida = INICIO.plusMinutes(20);
            assertThat(checkin(INICIO, saida).isPresencaQualificada(INICIO, fim)).isFalse();
        }

        @Test
        @DisplayName("fica 74,9% - logo abaixo do limite")
        void logoAbaixoDoLimite() {
            LocalDateTime fim = INICIO.plusHours(4);
            LocalDateTime saida = INICIO.plusHours(2).plusMinutes(59);
            assertThat(checkin(INICIO, saida).isPresencaQualificada(INICIO, fim)).isFalse();
        }

        @Test
        @DisplayName("evento com duracao zero nao qualifica ninguem (evita divisao por zero)")
        void duracaoZero() {
            assertThat(checkin(INICIO, INICIO).isPresencaQualificada(INICIO, INICIO)).isFalse();
        }

        @Test
        @DisplayName("evento com fim antes do inicio (cadastro invalido) nao qualifica")
        void duracaoNegativa() {
            LocalDateTime fim = INICIO.minusHours(1);
            assertThat(checkin(INICIO, INICIO.plusHours(2)).isPresencaQualificada(INICIO, fim)).isFalse();
        }
    }
}
