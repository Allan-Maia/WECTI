package com.wecti.api.controller;

import com.wecti.api.domain.Perfil;
import com.wecti.api.dto.NovoUsuarioRequest;
import com.wecti.api.dto.UsuarioResponse;
import com.wecti.api.security.AuthenticatedUser;
import com.wecti.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar(@RequestParam(required = false) Perfil perfil) {
        return usuarioService.listar(perfil).stream().map(UsuarioResponse::de).toList();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody NovoUsuarioRequest request) {
        var usuario = usuarioService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuario));
    }

    @GetMapping("/usuarios/me")
    public UsuarioResponse me(@AuthenticationPrincipal AuthenticatedUser autenticado) {
        return UsuarioResponse.de(usuarioService.buscarPorId(autenticado.id()));
    }
}
