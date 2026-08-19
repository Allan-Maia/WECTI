package com.wecti.api.service;

import com.wecti.api.dto.CadastroAlunoRequest;
import com.wecti.api.dto.LoginRequest;
import com.wecti.api.dto.LoginResponse;
import com.wecti.api.dto.RedefinirSenhaRequest;
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
    private final UsuarioService usuarioService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
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

    /** Cadastro publico (sempre ALUNO - ver UsuarioService.registrarAluno)
     *  ja devolve token, pra entrar direto no sistema sem precisar de um
     *  segundo login logo em seguida. */
    public LoginResponse registrar(CadastroAlunoRequest request) {
        var usuario = usuarioService.registrarAluno(request);
        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(token, UsuarioResponse.de(usuario));
    }

    /** "Esqueci minha senha" (ver UsuarioService.redefinirSenha) - ja
     *  devolve token, entao a pessoa entra direto com a senha nova. */
    public LoginResponse redefinirSenha(RedefinirSenhaRequest request) {
        var usuario = usuarioService.redefinirSenha(request);
        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(token, UsuarioResponse.de(usuario));
    }
}
