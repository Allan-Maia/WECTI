package com.wecti.api.service;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.Usuario;
import com.wecti.api.dto.NovoUsuarioRequest;
import com.wecti.api.exception.CampoInvalidoException;
import com.wecti.api.exception.ConflitoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar(Perfil perfil) {
        if (perfil != null) {
            return usuarioRepository.findByPerfil(perfil);
        }
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + id));
    }

    public Usuario criar(NovoUsuarioRequest request) {
        if (request.perfil() == Perfil.ALUNO && (request.rgm() == null || request.rgm().isBlank())) {
            throw new CampoInvalidoException("rgm", "RGM e obrigatorio para usuarios com perfil ALUNO");
        }

        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflitoException("Ja existe um usuario cadastrado com este email");
        }

        if (request.rgm() != null && usuarioRepository.findByRgm(request.rgm()).isPresent()) {
            throw new ConflitoException("Ja existe um usuario cadastrado com este RGM");
        }

        String senhaProvisoria = UUID.randomUUID().toString();
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .perfil(request.perfil())
                .rgm(request.perfil() == Perfil.ALUNO ? request.rgm() : null)
                .senha(passwordEncoder.encode(senhaProvisoria))
                .build();

        return usuarioRepository.save(usuario);
    }
}
