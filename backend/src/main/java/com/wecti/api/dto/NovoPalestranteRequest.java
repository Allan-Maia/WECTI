package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;

public record NovoPalestranteRequest(@NotBlank String nome, String bio) {
}
