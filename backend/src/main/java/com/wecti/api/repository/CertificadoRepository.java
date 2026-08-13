package com.wecti.api.repository;

import com.wecti.api.domain.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificadoRepository extends JpaRepository<Certificado, UUID> {
    Optional<Certificado> findByInscricaoId(UUID inscricaoId);
}
