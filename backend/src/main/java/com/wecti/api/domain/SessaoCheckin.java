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
 * QR code de check-in/check-out gerado pelo admin por EVENTO (nao mais
 * por aluno) - o proprio aluno escaneia com o celular e confirma sua
 * presenca. Um evento tem uma sessao do tipo ENTRADA e, depois, uma do
 * tipo SAIDA (cada uma pode ser regenerada, as antigas simplesmente
 * expiram e deixam de ser aceitas).
 *
 * O QR NAO carrega so o id da sessao: carrega tambem um codigo rotativo
 * derivado do {@link #segredo} (ver CodigoRotativoCheckin). Isso existe
 * porque o link e o mesmo pra sala inteira - sem rotacao, um print da
 * tela mandado no grupo valeria presenca pra quem nao veio.
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

    /**
     * Chave usada pra derivar o codigo rotativo do QR. Aleatoria por
     * sessao e nunca exposta em DTO nenhum - quem tem o segredo consegue
     * gerar codigos validos sem estar na sala.
     */
    @Column(nullable = false, length = 64)
    private String segredo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    /**
     * Fim da janela em que a sessao aceita confirmacoes. Nao e mais
     * "criacao + N horas": e o fim do proprio evento (com uma tolerancia
     * configuravel), pra que um QR gerado de manha nao continue valendo a
     * tarde. O inicio da janela e calculado a partir do evento em
     * CheckinSessaoService.
     */
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
