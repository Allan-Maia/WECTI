package com.wecti.api.repository;

import com.wecti.api.domain.PontuacaoExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PontuacaoExtraRepository extends JpaRepository<PontuacaoExtra, UUID> {

    /** Lancamentos de um aluno - tela de pontuacao do aluno e historico
     *  que o admin ve antes de lancar mais pontos. */
    List<PontuacaoExtra> findByAlunoIdOrderByCriadoEmDesc(UUID alunoId);

    /** Todos os lancamentos de uma vez, com o aluno carregado - o ranking
     *  soma os extras da turma inteira e nao pode consultar aluno a aluno. */
    @Query("select e from PontuacaoExtra e join fetch e.aluno")
    List<PontuacaoExtra> findTodosParaRanking();
}
