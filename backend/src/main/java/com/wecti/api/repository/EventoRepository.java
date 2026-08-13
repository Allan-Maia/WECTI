package com.wecti.api.repository;

import com.wecti.api.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    List<Evento> findByPeriodoId(UUID periodoId);
    List<Evento> findByDataHoraInicioAfter(LocalDateTime momento);
    List<Evento> findByDataHoraFimBefore(LocalDateTime momento);
}
