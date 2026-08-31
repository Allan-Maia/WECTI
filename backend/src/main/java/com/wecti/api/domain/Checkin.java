package com.wecti.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checkins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "inscricao_id", nullable = false, unique = true)
    private Inscricao inscricao;

    @Column(nullable = false)
    private LocalDateTime entrada;

    private LocalDateTime saida;

    /**
     * Regra do certificado/pontuacao: precisa de check-out registrado e
     * permanencia >= 75% da duracao do evento. Calculado em runtime, nao
     * persistido como coluna.
     *
     * Usa segundos (toSeconds()), nao minutos (toMinutes()) - toMinutes()
     * TRUNCA a fracao de minuto, o que da um erro enorme em eventos
     * curtos: um aluno com 3min53s de permanenca num evento de 5min tem
     * 77,7% de presenca de verdade (233s / 300s), mas toMinutes() truncava
     * pra "3 min de 5 min" = 60%, reprovando presenca que deveria passar.
     * Em eventos longos o erro de toMinutes() e desprezivel (no maximo 59s
     * de diferenca), mas nao ha motivo pra manter a imprecisao.
     */
    @Transient
    public boolean isPresencaQualificada(LocalDateTime inicioEvento, LocalDateTime fimEvento) {
        if (saida == null) {
            return false;
        }
        long duracaoEvento = Duration.between(inicioEvento, fimEvento).toSeconds();
        if (duracaoEvento <= 0) {
            return false;
        }
        long permanencia = Duration.between(entrada, saida).toSeconds();
        return ((double) permanencia / duracaoEvento) >= 0.75;
    }
}
