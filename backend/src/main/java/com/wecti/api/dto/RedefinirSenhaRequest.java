package com.wecti.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * "Esqueci minha senha" - sem link/token por email de proposito (o
 * professor confirmou que nao precisa de autenticacao extra, e o
 * projeto nao tem servidor de email configurado). A identidade e
 * confirmada com email + RGM (aluno) ou CPF (professor) - o mesmo
 * identificador ja usado no cadastro - e a pessoa define a senha nova na
 * hora. Serve tambem pra Professor (sempre cadastrado pelo Admin com uma
 * senha provisoria que ninguem sabe) definir a primeira senha de verdade.
 */
public record RedefinirSenhaRequest(
        @NotBlank @Email String email,
        @NotBlank String identificador,
        @NotBlank @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres") String novaSenha) {
}
