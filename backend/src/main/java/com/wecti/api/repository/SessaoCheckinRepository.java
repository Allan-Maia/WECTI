package com.wecti.api.repository;

import com.wecti.api.domain.SessaoCheckin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessaoCheckinRepository extends JpaRepository<SessaoCheckin, UUID> {
}
