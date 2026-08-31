package com.wecti.api.controller;

import com.wecti.api.dto.EventoResponse;
import com.wecti.api.dto.NovoEventoRequest;
import com.wecti.api.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public List<EventoResponse> listar(@RequestParam(required = false) String status) {
        return eventoService.listarComVagas(status);
    }

    @GetMapping("/{eventoId}")
    public EventoResponse detalhar(@PathVariable UUID eventoId) {
        return eventoService.detalharComVagas(eventoId);
    }

    @PostMapping
    public ResponseEntity<EventoResponse> criar(@Valid @RequestBody NovoEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criar(request));
    }

    @PutMapping("/{eventoId}")
    public EventoResponse atualizar(@PathVariable UUID eventoId, @Valid @RequestBody NovoEventoRequest request) {
        return eventoService.atualizar(eventoId, request);
    }

    @DeleteMapping("/{eventoId}")
    public ResponseEntity<Void> cancelar(@PathVariable UUID eventoId) {
        eventoService.cancelar(eventoId);
        return ResponseEntity.noContent().build();
    }
}
