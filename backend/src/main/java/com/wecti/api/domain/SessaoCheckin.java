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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QR code de check-in/check-out gerado pelo admin/professor por EVENTO
 * (nao mais por aluno) - o proprio aluno escaneia com o celular e
 * confirma sua presenca. Um evento tem uma sessao do tipo ENTRADA e,
 * depois, uma do tipo SAIDA (cada uma pode ser regenerada, as antigas
 * simplesmente expiram e deixam de ser aceitas).
 */
@Entity
@Table(name = "sessoes_checkin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSessaoCheckin tipo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @PrePersist
    void aoPersistir() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
    }

    public boolean isExpirada() {
        return LocalDateTime.now().isAfter(expiraEm);
    }
}
