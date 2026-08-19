package com.wecti.api.controller;

import com.wecti.api.dto.CheckinResponse;
import com.wecti.api.dto.NovaSessaoCheckinRequest;
import com.wecti.api.dto.SessaoCheckinResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.CheckinSessaoService;
import com.wecti.api.service.QrCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CheckinSessaoController {

    private final CheckinSessaoService checkinSessaoService;
    private final QrCodeService qrCodeService;
    private final String frontendUrl;

    public CheckinSessaoController(CheckinSessaoService checkinSessaoService, QrCodeService qrCodeService,
                                    @Value("${app.frontend-url}") String frontendUrl) {
        this.checkinSessaoService = checkinSessaoService;
        this.qrCodeService = qrCodeService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/eventos/{eventoId}/checkin-sessoes")
    public ResponseEntity<SessaoCheckinResponse> criar(@PathVariable UUID eventoId,
                                                        @Valid @RequestBody NovaSessaoCheckinRequest request) {
        var sessao = checkinSessaoService.criar(eventoId, request.tipo());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessaoCheckinResponse.de(sessao));
    }

    @GetMapping("/checkin-sessoes/{sessaoId}/qrcode")
    public ResponseEntity<byte[]> qrcode(@PathVariable UUID sessaoId) {
        checkinSessaoService.buscarValida(sessaoId);
        String url = frontendUrl + "/checkin/confirmar/" + sessaoId;
        byte[] png = qrCodeService.gerarPng(url);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @PostMapping("/checkin-sessoes/{sessaoId}/confirmar")
    public CheckinResponse confirmar(@PathVariable UUID sessaoId,
                                      @AuthenticationPrincipal AuthenticatedUser autenticado) {
        var checkin = checkinSessaoService.confirmar(sessaoId, autenticado.id());
        return CheckinResponse.de(checkin, checkin.getInscricao().getEvento());
    }
}
