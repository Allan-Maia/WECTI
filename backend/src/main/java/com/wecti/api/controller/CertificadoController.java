package com.wecti.api.controller;

import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.CertificadoService;
import com.wecti.api.service.InscricaoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CertificadoController {

    private final InscricaoService inscricaoService;
    private final CertificadoService certificadoService;

    public CertificadoController(InscricaoService inscricaoService, CertificadoService certificadoService) {
        this.inscricaoService = inscricaoService;
        this.certificadoService = certificadoService;
    }

    @GetMapping("/inscricoes/{inscricaoId}/certificado")
    public ResponseEntity<byte[]> emitirCertificado(@PathVariable UUID inscricaoId,
                                                      @AuthenticationPrincipal AuthenticatedUser autenticado) {
        var inscricao = inscricaoService.buscarComPermissao(inscricaoId, autenticado.id());
        byte[] pdf = certificadoService.emitir(inscricao);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
