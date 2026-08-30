package com.wecti.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Pontos lancados a mao pelo admin - tipicamente premio de gincana feita
 * durante uma palestra.
 *
 * <p>Nao substituem a pontuacao das palestras: somam-se a ela dentro do
 * mesmo periodo. Por isso ficam presos a um {@link Periodo}, igual ao
 * resto da pontuacao, e nao a um saldo unico do aluno.
 *
 * <p>Cada lancamento guarda quem lancou e por que. Num ranking que vai
 * definir premiados no fim do WECTI, "de onde vieram esses 50 pontos" e
 * uma pergunta que alguem vai fazer.
 */
@Entity
@Table(name = "pontuacoes_extras")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PontuacaoExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    /** Pode ser negativo - e como o admin corrige um lancamento a maior
     *  sem apagar o historico. */
    @Column(nullable = false)
    private Integer pontos;

    @Column(nullable = false, length = 200)
    private String motivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoPersistir() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
