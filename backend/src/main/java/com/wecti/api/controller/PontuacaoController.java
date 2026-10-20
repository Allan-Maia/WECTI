package com.wecti.api.controller;

import com.wecti.api.dto.PontuacaoResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.PontuacaoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PontuacaoController {

    private final PontuacaoService pontuacaoService;

    public PontuacaoController(PontuacaoService pontuacaoService) {
        this.pontuacaoService = pontuacaoService;
    }

    @GetMapping("/me/pontuacao")
    public PontuacaoResponse minhaPontuacao(@AuthenticationPrincipal AuthenticatedUser autenticado) {
        return pontuacaoService.calcular(autenticado.id());
    }

    /** Pontuacao de um aluno especifico - painel do admin. */
    @GetMapping("/pontuacao/aluno/{alunoId}")
    public PontuacaoResponse pontuacaoDoAluno(@PathVariable UUID alunoId) {
        return pontuacaoService.calcular(alunoId);
    }
}
