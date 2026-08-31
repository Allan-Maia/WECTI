package com.wecti.api.controller;

import com.wecti.api.dto.NovaPontuacaoExtraRequest;
import com.wecti.api.dto.PontuacaoExtraResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.PontuacaoExtraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Lançamento manual de pontos (gincanas). Só admin — ver SecurityConfig.
 * O aluno vê os próprios lançamentos dentro de GET /me/pontuacao, com o
 * motivo de cada um.
 */
@RestController
@RequestMapping("/pontuacao-extra")
public class PontuacaoExtraController {

    private final PontuacaoExtraService service;

    public PontuacaoExtraController(PontuacaoExtraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PontuacaoExtraResponse> lancar(
            @Valid @RequestBody NovaPontuacaoExtraRequest request,
            @AuthenticationPrincipal AuthenticatedUser autenticado) {
        var extra = service.lancar(request, autenticado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(PontuacaoExtraResponse.de(extra));
    }

    /** Histórico de um aluno - o admin confere antes de lançar de novo,
     *  para não premiar duas vezes a mesma gincana. */
    @GetMapping
    public List<PontuacaoExtraResponse> listar(@RequestParam(name = "aluno_id") UUID alunoId) {
        return service.listar(alunoId).stream().map(PontuacaoExtraResponse::de).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
