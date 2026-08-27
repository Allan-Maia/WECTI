package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.Periodo;
import com.wecti.api.domain.Usuario;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.PeriodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pontuacao do aluno no periodo. Nao ha tabela de saldo: o valor e
 * recalculado a cada consulta a partir de Inscricao/Checkin/Evento, entao
 * estes testes cobrem a regra inteira, nao um cache.
 *
 * Regras cobertas:
 *  - so pontua quem cumpre o MESMO criterio do certificado (75%);
 *  - quem nao cancelou e nao compareceu perde os pontos do evento (no-show);
 *  - inscricao cancelada nao pontua nem penaliza;
 *  - evento que ainda nao terminou nao entra na conta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PontuacaoServiceTest {

    private static final UUID ALUNO_ID = UUID.randomUUID();
    private static final int PONTOS_DO_EVENTO = 100;

    @Mock private PeriodoRepository periodoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private InscricaoRepository inscricaoRepository;
    @Mock private CheckinRepository checkinRepository;

    @InjectMocks private PontuacaoService pontuacaoService;

    private Periodo periodo;

    @BeforeEach
    void preparar() {
        periodo = Periodo.builder()
                .id(UUID.randomUUID())
                .nome("2026.2")
                .dataInicio(LocalDate.now().minusMonths(1))
                .dataFim(LocalDate.now().plusMonths(3))
                .build();
        when(periodoRepository.findById(periodo.getId())).thenReturn(Optional.of(periodo));
    }

    /** Evento de 2h que ja terminou ontem. */
    private Evento eventoEncerrado() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(1).withHour(19).withMinute(0);
        return Evento.builder()
                .id(UUID.randomUUID())
                .periodo(periodo)
                .titulo("Palestra de teste")
                .dataHoraInicio(inicio)
                .dataHoraFim(inicio.plusHours(2))
                .pontos(PONTOS_DO_EVENTO)
                .build();
    }

    private Inscricao inscricao(Evento evento, InscricaoStatus status) {
        return Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(Usuario.builder().id(ALUNO_ID).build())
                .evento(evento)
                .status(status)
                .build();
    }

    private void cenario(Evento evento, Inscricao inscricao, Checkin checkin) {
        when(eventoRepository.findByPeriodoId(periodo.getId())).thenReturn(List.of(evento));
        when(inscricaoRepository.findByAlunoIdAndEventoId(ALUNO_ID, evento.getId()))
                .thenReturn(Optional.ofNullable(inscricao));
        when(checkinRepository.findByInscricaoId(any())).thenReturn(Optional.ofNullable(checkin));
    }

    @Test
    @DisplayName("presenca completa credita os pontos do evento")
    void presencaCompletaPontua() {
        Evento evento = eventoEncerrado();
        Inscricao inscricao = inscricao(evento, InscricaoStatus.ATIVA);
        Checkin checkin = Checkin.builder()
                .inscricao(inscricao)
                .entrada(evento.getDataHoraInicio())
                .saida(evento.getDataHoraFim())
                .build();
        cenario(evento, inscricao, checkin);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal()).isEqualTo(PONTOS_DO_EVENTO);
        assertThat(resultado.eventos()).singleElement()
                .satisfies(item -> {
                    assertThat(item.pontos()).isEqualTo(PONTOS_DO_EVENTO);
                    assertThat(item.status()).isEqualTo("concluido");
                });
    }

    @Test
    @DisplayName("so check-in, sem check-out, nao pontua - mas tambem nao penaliza")
    void semCheckoutNaoPontua() {
        Evento evento = eventoEncerrado();
        Inscricao inscricao = inscricao(evento, InscricaoStatus.ATIVA);
        Checkin checkin = Checkin.builder()
                .inscricao(inscricao)
                .entrada(evento.getDataHoraInicio())
                .saida(null)
                .build();
        cenario(evento, inscricao, checkin);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal()).isZero();
        assertThat(resultado.eventos()).singleElement()
                .satisfies(item -> assertThat(item.pontos()).isZero());
    }

    @Test
    @DisplayName("saiu antes dos 75% nao pontua")
    void saidaAntecipadaNaoPontua() {
        Evento evento = eventoEncerrado();
        Inscricao inscricao = inscricao(evento, InscricaoStatus.ATIVA);
        Checkin checkin = Checkin.builder()
                .inscricao(inscricao)
                .entrada(evento.getDataHoraInicio())
                .saida(evento.getDataHoraInicio().plusMinutes(30)) // 30min de 2h = 25%
                .build();
        cenario(evento, inscricao, checkin);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal()).isZero();
    }

    @Test
    @DisplayName("no-show: inscrito, evento encerrado e nenhum check-in - perde os pontos do evento")
    void noShowPenaliza() {
        Evento evento = eventoEncerrado();
        Inscricao inscricao = inscricao(evento, InscricaoStatus.ATIVA);
        cenario(evento, inscricao, null);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal())
                .as("a penalidade e exatamente o valor que o evento valeria")
                .isEqualTo(-PONTOS_DO_EVENTO);
        assertThat(resultado.eventos()).singleElement()
                .satisfies(item -> assertThat(item.status()).isEqualTo("no_show"));
    }

    @Test
    @DisplayName("quem cancelou a tempo nao pontua nem e penalizado")
    void canceladaNaoPenaliza() {
        Evento evento = eventoEncerrado();
        Inscricao inscricao = inscricao(evento, InscricaoStatus.CANCELADA);
        cenario(evento, inscricao, null);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal())
                .as("cancelar e diferente de faltar - nao pode virar no-show")
                .isZero();
        assertThat(resultado.eventos()).singleElement()
                .satisfies(item -> assertThat(item.status()).isEqualTo("cancelado"));
    }

    @Test
    @DisplayName("evento que ainda nao terminou nao entra na conta")
    void eventoFuturoNaoConta() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(3);
        Evento evento = Evento.builder()
                .id(UUID.randomUUID()).periodo(periodo).titulo("Ainda vai acontecer")
                .dataHoraInicio(inicio).dataHoraFim(inicio.plusHours(2))
                .pontos(PONTOS_DO_EVENTO).build();
        Inscricao inscricao = inscricao(evento, InscricaoStatus.ATIVA);
        cenario(evento, inscricao, null);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal())
                .as("inscrito num evento futuro nao pode ser tratado como faltoso")
                .isZero();
        assertThat(resultado.eventos()).isEmpty();
    }

    @Test
    @DisplayName("evento em que o aluno nem se inscreveu e ignorado")
    void semInscricaoIgnora() {
        Evento evento = eventoEncerrado();
        cenario(evento, null, null);

        var resultado = pontuacaoService.calcular(ALUNO_ID, periodo.getId());

        assertThat(resultado.pontosTotal()).isZero();
        assertThat(resultado.eventos()).isEmpty();
    }
}
