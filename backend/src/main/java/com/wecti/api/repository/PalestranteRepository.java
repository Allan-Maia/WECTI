package com.wecti.api.repository;

import com.wecti.api.domain.Palestrante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PalestranteRepository extends JpaRepository<Palestrante, UUID> {
}
