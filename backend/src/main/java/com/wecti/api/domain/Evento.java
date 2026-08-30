package com.wecti.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eventos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 200)
    private String local;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    /**
     * Pontos que o evento vale para o aluno que cumprir check-in +
     * check-out + permanencia >= 75% da duracao. O mesmo valor e
     * descontado em caso de no-show (ver regra de negocio no contrato
     * de API, docs/openapi.yaml).
     */
    @Column(nullable = false)
    private Integer pontos;

    /**
     * Numero maximo de inscricoes ATIVAS. {@code null} significa sem
     * limite - e o que descreve os eventos criados antes desta regra
     * existir, e continua util para evento sem restricao de espaco.
     *
     * <p>So conta inscricao ativa: quem cancela devolve a vaga para a
     * fila. Ver InscricaoService.inscrever.
     */
    @Column
    private Integer capacidade;

    /**
     * EAGER de proposito: o controller mapeia Evento -> EventoResponse
     * fora de uma transacao (open-in-view esta desligado), e @ManyToMany
     * e LAZY por padrao no Hibernate - acessar essa colecao depois que a
     * sessao ja fechou (ex.: ao listar eventos) derrubava a request com
     * LazyInitializationException. Sao poucos palestrantes por evento, o
     * custo de sempre carregar junto e desprezivel.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "evento_palestrante",
            joinColumns = @JoinColumn(name = "evento_id"),
            inverseJoinColumns = @JoinColumn(name = "palestrante_id")
    )
    private Set<Palestrante> palestrantes = new HashSet<>();
}
