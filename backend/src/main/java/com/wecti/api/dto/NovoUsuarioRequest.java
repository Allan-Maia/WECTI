package com.wecti.api.dto;

import com.wecti.api.domain.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NovoUsuarioRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotNull Perfil perfil,
        @Pattern(regexp = "^\\d{8}$", message = "RGM deve ter exatamente 8 digitos") String rgm) {
}
