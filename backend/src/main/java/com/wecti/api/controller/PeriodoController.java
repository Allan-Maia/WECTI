package com.wecti.api.controller;

import com.wecti.api.dto.NovoPeriodoRequest;
import com.wecti.api.dto.PeriodoResponse;
import com.wecti.api.service.PeriodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/periodos")
public class PeriodoController {

    private final PeriodoService periodoService;

    public PeriodoController(PeriodoService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    public List<PeriodoResponse> listar() {
        return periodoService.listar().stream().map(PeriodoResponse::de).toList();
    }

    @PostMapping
    public ResponseEntity<PeriodoResponse> criar(@Valid @RequestBody NovoPeriodoRequest request) {
        var periodo = periodoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PeriodoResponse.de(periodo));
    }
}
