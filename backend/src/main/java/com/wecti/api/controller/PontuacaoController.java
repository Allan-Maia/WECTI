package com.wecti.api.controller;

import com.wecti.api.dto.PontuacaoPeriodoResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.PontuacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PontuacaoController {

    private final PontuacaoService pontuacaoService;

    public PontuacaoController(PontuacaoService pontuacaoService) {
        this.pontuacaoService = pontuacaoService;
    }

    @GetMapping("/me/pontuacao")
    public PontuacaoPeriodoResponse minhaPontuacao(@AuthenticationPrincipal AuthenticatedUser autenticado,
                                                     @RequestParam(required = false, name = "periodo_id") UUID periodoId) {
        return pontuacaoService.calcular(autenticado.id(), periodoId);
    }

    @GetMapping("/pontuacao/aluno/{alunoId}/periodo/{periodoId}")
    public PontuacaoPeriodoResponse pontuacaoDoAluno(@PathVariable UUID alunoId, @PathVariable UUID periodoId) {
        return pontuacaoService.calcular(alunoId, periodoId);
    }
}
