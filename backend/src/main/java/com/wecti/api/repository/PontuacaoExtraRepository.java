package com.wecti.api.repository;

import com.wecti.api.domain.PontuacaoExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PontuacaoExtraRepository extends JpaRepository<PontuacaoExtra, UUID> {

    /** Lancamentos de um aluno no periodo - tela de pontuacao do aluno e
     *  historico que o admin ve antes de lancar mais pontos. */
    List<PontuacaoExtra> findByAlunoIdAndPeriodoIdOrderByCriadoEmDesc(UUID alunoId, UUID periodoId);

    /** Todos os lancamentos do periodo, de uma vez - o ranking soma os
     *  extras da turma inteira e nao pode consultar aluno a aluno. */
    List<PontuacaoExtra> findByPeriodoId(UUID periodoId);
}
