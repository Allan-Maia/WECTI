package com.wecti.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro publico (tela de login, "Primeiro acesso? Crie sua conta") -
 * sem campo perfil de proposito: quem se autocadastra vira ALUNO sempre,
 * hardcoded no AuthService/UsuarioService, nunca a partir do que o
 * cliente manda (mesmo que alguem tente forjar um "perfil": "ADMIN" no
 * corpo da requisicao, o backend ignora e usa ALUNO). Professor continua
 * so sendo cadastrado pelo Admin, na tela interna de Usuarios.
 */
public record CadastroAlunoRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres") String senha,
        @NotBlank @Pattern(regexp = "^\\d{8}$", message = "RGM deve ter exatamente 8 digitos") String rgm,
        String curso) {
}
