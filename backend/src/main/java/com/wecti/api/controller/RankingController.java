package com.wecti.api.controller;

import com.wecti.api.domain.Perfil;
import com.wecti.api.dto.RankingResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.RankingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classificação por pontos. Uma rota só para os dois perfis: a
 * diferença é o que volta em cada uma.
 *
 * <p>O RGM só é incluído para o admin, que precisa dele para não lançar
 * pontos no aluno errado. Para o aluno, o ranking mostra nome e curso —
 * o suficiente para acompanhar a disputa, sem espalhar o identificador
 * acadêmico dos colegas por uma tela que a turma inteira abre. Quem
 * decide isso é o perfil do token, nunca um parâmetro da requisição.
 */
@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/ranking")
    public RankingResponse ranking(@AuthenticationPrincipal AuthenticatedUser autenticado) {
        return rankingService.montar(autenticado.perfil() == Perfil.ADMIN);
    }
}
