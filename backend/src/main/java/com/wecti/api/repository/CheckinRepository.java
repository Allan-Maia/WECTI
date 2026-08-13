package com.wecti.api.repository;

import com.wecti.api.domain.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<Checkin, UUID> {
    Optional<Checkin> findByInscricaoId(UUID inscricaoId);
}
