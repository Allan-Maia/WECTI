package com.wecti.api.dto;

import com.wecti.api.domain.Palestrante;

import java.util.UUID;

public record PalestranteResponse(UUID id, String nome, String bio) {

    public static PalestranteResponse de(Palestrante palestrante) {
        return new PalestranteResponse(palestrante.getId(), palestrante.getNome(), palestrante.getBio());
    }
}
