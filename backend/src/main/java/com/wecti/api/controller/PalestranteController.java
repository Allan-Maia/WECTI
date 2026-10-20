package com.wecti.api.controller;

import com.wecti.api.dto.NovoPalestranteRequest;
import com.wecti.api.dto.PalestranteResponse;
import com.wecti.api.service.PalestranteService;
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
@RequestMapping("/palestrantes")
public class PalestranteController {

    private final PalestranteService palestranteService;

    public PalestranteController(PalestranteService palestranteService) {
        this.palestranteService = palestranteService;
    }

    @GetMapping
    public List<PalestranteResponse> listar() {
        return palestranteService.listar().stream().map(PalestranteResponse::de).toList();
    }

    @PostMapping
    public ResponseEntity<PalestranteResponse> criar(@Valid @RequestBody NovoPalestranteRequest request) {
        var palestrante = palestranteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PalestranteResponse.de(palestrante));
    }
}
