package com.wecti.api.controller;

import com.wecti.api.dto.CadastroAlunoRequest;
import com.wecti.api.dto.LoginRequest;
import com.wecti.api.dto.LoginResponse;
import com.wecti.api.dto.RedefinirSenhaRequest;
import com.wecti.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Cadastro publico (tela de login, "Primeiro acesso? Crie sua conta")
     *  - sempre cria ALUNO, ver CadastroAlunoRequest/AuthService.registrar.
     *  Professor continua sendo cadastrado so pelo Admin. */
    @PostMapping("/registrar")
    public ResponseEntity<LoginResponse> registrar(@Valid @RequestBody CadastroAlunoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** "Esqueci minha senha" - ver RedefinirSenhaRequest/AuthService. */
    @PostMapping("/redefinir-senha")
    public LoginResponse redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return authService.redefinirSenha(request);
    }
}
