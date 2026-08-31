package com.wecti.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "inscricao_id", nullable = false, unique = true)
    private Inscricao inscricao;

    /**
     * Codigo publico de validacao (ex.: "WCT-2026-A7F3K2") - impresso no
     * PDF e resolvido por GET /validar/{codigo}. Gerado pelo
     * CodigoCertificadoGenerator; ver V4__codigo_validacao_certificado.sql
     * sobre a coluna ser nullable no banco.
     */
    @Column(length = 20, unique = true)
    private String codigo;

    @Column(name = "emitido_em", nullable = false)
    private LocalDateTime emitidoEm;

    @Column(name = "url_pdf", length = 500)
    private String urlPdf;

    @PrePersist
    void aoPersistir() {
        if (emitidoEm == null) {
            emitidoEm = LocalDateTime.now();
        }
    }
}
