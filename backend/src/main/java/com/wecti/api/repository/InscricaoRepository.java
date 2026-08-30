package com.wecti.api.repository;

import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {
    List<Inscricao> findByAlunoId(UUID alunoId);
    List<Inscricao> findByAlunoIdAndStatus(UUID alunoId, InscricaoStatus status);
    List<Inscricao> findByEventoId(UUID eventoId);
    Optional<Inscricao> findByAlunoIdAndEventoId(UUID alunoId, UUID eventoId);
    boolean existsByAlunoIdAndEventoId(UUID alunoId, UUID eventoId);
    boolean existsByAlunoId(UUID alunoId);

    /** Vagas ocupadas de um evento. So ATIVA conta - quem cancelou
     *  devolveu a vaga. */
    long countByEventoIdAndStatus(UUID eventoId, InscricaoStatus status);

    /**
     * Ocupacao de varios eventos numa consulta so. A lista de eventos
     * mostra "X de Y vagas" em cada linha; contar um a um faria uma
     * consulta por evento na tela.
     */
    @Query("""
            select i.evento.id, count(i)
              from Inscricao i
             where i.status = :status
               and i.evento.id in :eventoIds
             group by i.evento.id
            """)
    List<Object[]> contarPorEvento(@Param("eventoIds") Collection<UUID> eventoIds,
                                    @Param("status") InscricaoStatus status);

    /** Todas as inscricoes dos eventos de um periodo - usado pelo
     *  ranking, que precisa da turma inteira de uma vez. */
    @Query("select i from Inscricao i where i.evento.periodo.id = :periodoId")
    List<Inscricao> findByPeriodoId(@Param("periodoId") UUID periodoId);
}
