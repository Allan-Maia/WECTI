package com.wecti.api.repository;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// JpaSpecificationExecutor: usado pra combinar os filtros dinamicos
// (perfil + busca por texto) da paginacao de GET /usuarios - ver
// UsuarioService.listar.
public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByRgm(String rgm);
    Optional<Usuario> findByCpf(String cpf);
    List<Usuario> findByPerfil(Perfil perfil);
}
