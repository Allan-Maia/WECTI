package com.wecti.api.repository;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByRgm(String rgm);
    List<Usuario> findByPerfil(Perfil perfil);
}
