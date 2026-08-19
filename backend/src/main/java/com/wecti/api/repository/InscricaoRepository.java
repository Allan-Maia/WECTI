package com.wecti.api.repository;

import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
