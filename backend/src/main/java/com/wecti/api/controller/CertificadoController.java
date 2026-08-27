package com.wecti.api.controller;

import com.wecti.api.dto.CertificadoValidacaoResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.CertificadoService;
import com.wecti.api.service.InscricaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
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
    private final String frontendUrl;

    public CertificadoController(InscricaoService inscricaoService, CertificadoService certificadoService,
                                  @Value("${app.frontend-url}") String frontendUrl) {
        this.inscricaoService = inscricaoService;
        this.certificadoService = certificadoService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/inscricoes/{inscricaoId}/certificado")
    public ResponseEntity<byte[]> emitirCertificado(@PathVariable UUID inscricaoId,
                                                      @AuthenticationPrincipal AuthenticatedUser autenticado,
                                                      HttpServletRequest request) {
        var inscricao = inscricaoService.buscarComPermissao(inscricaoId, autenticado.id());
        byte[] pdf = certificadoService.emitir(inscricao, baseUrl(request));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    /**
     * Validacao PUBLICA (sem login): quem confere um certificado
     * costuma ser um recrutador ou outra instituicao, que nao tem conta
     * no sistema - exigir login inviabilizaria o proprio objetivo da
     * funcionalidade. A protecao aqui nao e o login, e o codigo ser
     * aleatorio e longo o bastante pra nao ser adivinhado (ver
     * CodigoCertificadoGenerator), e a resposta nao trazer dado que nao
     * seja necessario pra verificacao (ver CertificadoValidacaoResponse).
     */
    @GetMapping("/validar/{codigo}")
    public CertificadoValidacaoResponse validar(@PathVariable String codigo) {
        return certificadoService.validar(codigo);
    }

    /**
     * Mesmo motivo do QR de check-in: o link impresso no certificado
     * precisa apontar pro endereco que a pessoa realmente consegue abrir.
     * O Origin da requisicao e a URL do site que o aluno esta usando pra
     * baixar o certificado; "app.frontend-url" fica de reserva pra
     * chamadas sem Origin.
     */
    private String baseUrl(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return (origin != null && !origin.isBlank()) ? origin : frontendUrl;
    }
}
