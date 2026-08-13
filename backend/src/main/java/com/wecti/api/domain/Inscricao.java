package com.wecti.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscricoes", uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "evento_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InscricaoStatus status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    /**
     * Token unico usado para gerar o QR code enviado por e-mail. E o que
     * o app Android le para registrar check-in/check-out.
     */
    @Column(name = "qrcode_token", nullable = false, unique = true, length = 64)
    private String qrcodeToken;

    @PrePersist
    void aoPersistir() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
        if (status == null) {
            status = InscricaoStatus.ATIVA;
        }
    }
}
