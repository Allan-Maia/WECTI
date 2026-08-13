package com.wecti.api.dto;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.Usuario;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email, Perfil perfil, String rgm) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.getRgm());
    }
}
