package com.wecti.api.controller;

import com.wecti.api.domain.Perfil;
import com.wecti.api.dto.InscricaoResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.EmailService;
import com.wecti.api.service.InscricaoService;
import com.wecti.api.service.QrCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class InscricaoController {

    private final InscricaoService inscricaoService;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;

    public InscricaoController(InscricaoService inscricaoService, QrCodeService qrCodeService,
                                EmailService emailService) {
        this.inscricaoService = inscricaoService;
        this.qrCodeService = qrCodeService;
        this.emailService = emailService;
    }

    @PostMapping("/eventos/{eventoId}/inscricoes")
    public ResponseEntity<InscricaoResponse> inscrever(@PathVariable UUID eventoId,
                                                         @AuthenticationPrincipal AuthenticatedUser autenticado) {
        var inscricao = inscricaoService.inscrever(eventoId, autenticado.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricaoService.montarResposta(inscricao));
    }

    @GetMapping("/eventos/{eventoId}/inscricoes")
    public List<InscricaoResponse> listarPorEvento(@PathVariable UUID eventoId) {
        return inscricaoService.listarPorEvento(eventoId).stream()
                .map(inscricaoService::montarResposta)
                .toList();
    }

    @GetMapping("/me/inscricoes")
    public List<InscricaoResponse> minhasInscricoes(@AuthenticationPrincipal AuthenticatedUser autenticado,
                                                      @RequestParam(required = false) String status) {
        return inscricaoService.listarDoAluno(autenticado.id(), status).stream()
                .map(inscricaoService::montarResposta)
                .toList();
    }

    @GetMapping("/inscricoes/{inscricaoId}")
    public InscricaoResponse detalhar(@PathVariable UUID inscricaoId,
                                       @AuthenticationPrincipal AuthenticatedUser autenticado) {
        UUID alunoIdSeAluno = autenticado.perfil() == Perfil.ALUNO ? autenticado.id() : null;
        var inscricao = inscricaoService.buscarComPermissao(inscricaoId, alunoIdSeAluno);
        return inscricaoService.montarResposta(inscricao);
    }

    @DeleteMapping("/inscricoes/{inscricaoId}")
    public ResponseEntity<Void> cancelar(@PathVariable UUID inscricaoId,
                                          @AuthenticationPrincipal AuthenticatedUser autenticado) {
        inscricaoService.cancelar(inscricaoId, autenticado.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inscricoes/{inscricaoId}/qrcode")
    public ResponseEntity<byte[]> obterQrCode(@PathVariable UUID inscricaoId,
                                               @AuthenticationPrincipal AuthenticatedUser autenticado) {
        var inscricao = inscricaoService.buscarComPermissao(inscricaoId, autenticado.id());
        byte[] png = qrCodeService.gerarPng(inscricao.getQrcodeToken());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @PostMapping("/inscricoes/{inscricaoId}/qrcode")
    public ResponseEntity<Void> reenviarQrCode(@PathVariable UUID inscricaoId,
                                                @AuthenticationPrincipal AuthenticatedUser autenticado) {
        var inscricao = inscricaoService.buscarComPermissao(inscricaoId, autenticado.id());
        emailService.enviarQrCode(inscricao);
        return ResponseEntity.accepted().build();
    }
}
