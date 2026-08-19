package com.wecti.api.repository;

import com.wecti.api.domain.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PeriodoRepository extends JpaRepository<Periodo, UUID> {

    /** Periodos cujo intervalo [data_inicio, data_fim] contem a data
     *  informada - usado pelo EventoService pra descobrir sozinho em qual
     *  Periodo (semestre) um evento cai, a partir da propria data dele. */
    @Query("select p from Periodo p where :data between p.dataInicio and p.dataFim")
    List<Periodo> findQueContem(@Param("data") LocalDate data);
}
