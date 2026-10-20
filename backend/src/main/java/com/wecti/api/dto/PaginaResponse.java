package com.wecti.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope simples de paginacao pras listagens que crescem muito (ex.:
 * GET /usuarios) - devolvemos isso em vez do Page<> do Spring Data
 * diretamente porque a serializacao "de fabrica" do Page muda de versao
 * pra versao e carrega campo interno (pageable, sort) que nao faz
 * sentido expor no contrato da API.
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapeador) {
        return new PaginaResponse<>(
                page.getContent().stream().map(mapeador).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
