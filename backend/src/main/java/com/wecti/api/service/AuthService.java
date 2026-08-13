package com.wecti.api.service;

import com.wecti.api.dto.LoginRequest;
import com.wecti.api.dto.LoginResponse;
import com.wecti.api.dto.UsuarioResponse;
import com.wecti.api.exception.CredenciaisInvalidasException;
import com.wecti.api.repository.UsuarioRepository;
import com.wecti.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha invalidos"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Email ou senha invalidos");
        }

        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(token, UsuarioResponse.de(usuario));
    }
}
