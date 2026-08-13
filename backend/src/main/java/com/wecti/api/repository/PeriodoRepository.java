package com.wecti.api.repository;

import com.wecti.api.domain.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PeriodoRepository extends JpaRepository<Periodo, UUID> {
}
