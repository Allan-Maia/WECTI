package com.wecti.api.controller;

import com.wecti.api.dto.CheckinRequest;
import com.wecti.api.dto.CheckinResponse;
import com.wecti.api.service.CheckinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/checkins")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    public ResponseEntity<CheckinResponse> entrada(@Valid @RequestBody CheckinRequest request) {
        var checkin = checkinService.registrarEntrada(request.qrcodeToken());
        var resposta = CheckinResponse.de(checkin, checkin.getInscricao().getEvento());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/{checkinId}/checkout")
    public CheckinResponse saida(@PathVariable UUID checkinId) {
        var checkin = checkinService.registrarSaida(checkinId);
        return CheckinResponse.de(checkin, checkin.getInscricao().getEvento());
    }
}
