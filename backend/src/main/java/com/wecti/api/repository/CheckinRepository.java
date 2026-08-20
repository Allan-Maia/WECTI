package com.wecti.api.repository;

import com.wecti.api.domain.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {
    Optional<Checkin> findByInscricaoId(UUID inscricaoId);

    /** Relatório de presença (admin/professor) - todo aluno que já fez
     *  check-in nesse evento, mais cedo primeiro. */
    List<Checkin> findByInscricao_Evento_IdOrderByEntradaAsc(UUID eventoId);
}
