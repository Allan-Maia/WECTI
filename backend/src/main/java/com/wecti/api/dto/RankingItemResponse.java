package com.wecti.api.dto;

import java.util.UUID;

/**
 * Uma linha do ranking.
 *
 * @param posicao           empatados dividem a mesma posicao (1, 2, 2, 4)
 * @param alunoRgm          preenchido so na visao do admin - o aluno nao
 *                          precisa ver o RGM dos colegas para acompanhar
 *                          a disputa
 * @param pontosEventos     parcela vinda das palestras
 * @param pontosExtras      parcela lancada pelo admin (gincanas)
 * @param eventosConcluidos quantas palestras o aluno de fato pontuou -
 *                          separa quem tem 100 pontos de cinco palestras
 *                          de quem tem 100 de uma gincana so
 */
public record RankingItemResponse(
        int posicao,
        UUID alunoId,
        String alunoNome,
        String alunoRgm,
        String alunoCurso,
        int pontosEventos,
        int pontosExtras,
        int pontosTotal,
        int eventosConcluidos) {
}
