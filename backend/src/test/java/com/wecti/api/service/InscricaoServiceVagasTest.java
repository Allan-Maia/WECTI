package com.wecti.api.service;

import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.Usuario;
import com.wecti.api.exception.ConflitoException;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Controle de vagas por evento.
 *
 * <p>A regra que o professor pediu tem duas metades, e a segunda e a que
 * costuma faltar: <b>quem desiste devolve a vaga</b>. Por isso os testes
 * cobrem tanto "lotou, nao entra mais" quanto "cancelou, abriu vaga" -
 * inclusive para o proprio aluno que cancelou e mudou de ideia.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscricaoServiceVagasTest {

    private static final UUID ALUNO_ID = UUID.randomUUID();
    private static final UUID EVENTO_ID = UUID.randomUUID();

    @Mock private InscricaoRepository inscricaoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CheckinRepository checkinRepository;

    @InjectMocks private InscricaoService inscricaoService;

    @BeforeEach
    void preparar() {
        when(usuarioRepository.findById(ALUNO_ID))
                .thenReturn(Optional.of(Usuario.builder().id(ALUNO_ID).nome("Aluno").build()));
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(i -> i.getArgument(0));
        when(inscricaoRepository.findByAlunoIdAndEventoId(ALUNO_ID, EVENTO_ID)).thenReturn(Optional.empty());
    }

    /** Evento futuro com a capacidade informada ({@code null} = sem limite). */
    private void eventoCom(Integer capacidade, long jaInscritos) {
        Evento evento = Evento.builder()
                .id(EVENTO_ID)
                .titulo("Palestra")
                .dataHoraInicio(LocalDateTime.now().plusDays(3))
                .dataHoraFim(LocalDateTime.now().plusDays(3).plusHours(2))
                .pontos(100)
                .capacidade(capacidade)
                .build();
        when(eventoRepository.travarParaInscricao(EVENTO_ID.toString())).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndStatus(EVENTO_ID, InscricaoStatus.ATIVA)).thenReturn(jaInscritos);
    }

    @Test
    @DisplayName("inscreve normalmente quando ainda ha vaga")
    void inscreveComVagaDisponivel() {
        eventoCom(400, 399);

        Inscricao inscricao = inscricaoService.inscrever(EVENTO_ID, ALUNO_ID);

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.ATIVA);
    }

    @Test
    @DisplayName("recusa a inscricao quando as vagas acabaram")
    void recusaQuandoLotado() {
        eventoCom(400, 400);

        assertThatThrownBy(() -> inscricaoService.inscrever(EVENTO_ID, ALUNO_ID))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("esgotaram");

        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("evento sem capacidade definida nao tem limite")
    void semCapacidadeNaoLimita() {
        eventoCom(null, 5000);

        assertThat(inscricaoService.inscrever(EVENTO_ID, ALUNO_ID)).isNotNull();
    }

    @Test
    @DisplayName("quem cancelou devolveu a vaga: so inscricao ATIVA ocupa lugar")
    void cancelamentoDevolveVaga() {
        // 400 lugares, 400 inscritos no total, mas 5 cancelaram - a
        // contagem que o servico usa e a de ATIVA, entao sobram 5 vagas.
        eventoCom(400, 395);

        assertThat(inscricaoService.inscrever(EVENTO_ID, ALUNO_ID)).isNotNull();

        verify(inscricaoRepository).countByEventoIdAndStatus(EVENTO_ID, InscricaoStatus.ATIVA);
    }

    @Test
    @DisplayName("aluno que cancelou consegue voltar, reaproveitando a propria inscricao")
    void alunoQueCancelouPodeVoltar() {
        eventoCom(400, 10);
        Inscricao cancelada = Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(Usuario.builder().id(ALUNO_ID).build())
                .status(InscricaoStatus.CANCELADA)
                .canceladaEm(LocalDateTime.now().minusDays(1))
                .build();
        when(inscricaoRepository.findByAlunoIdAndEventoId(ALUNO_ID, EVENTO_ID)).thenReturn(Optional.of(cancelada));

        Inscricao volta = inscricaoService.inscrever(EVENTO_ID, ALUNO_ID);

        assertThat(volta.getId())
                .as("reaproveita a inscricao existente em vez de criar uma segunda para o mesmo par aluno/evento")
                .isEqualTo(cancelada.getId());
        assertThat(volta.getStatus()).isEqualTo(InscricaoStatus.ATIVA);
        assertThat(volta.getCanceladaEm())
                .as("inscricao ativa com data de cancelamento antiga confundiria qualquer relatorio")
                .isNull();
    }

    @Test
    @DisplayName("volta de quem cancelou tambem respeita a lotacao")
    void voltaRespeitaCapacidade() {
        eventoCom(400, 400);
        when(inscricaoRepository.findByAlunoIdAndEventoId(ALUNO_ID, EVENTO_ID))
                .thenReturn(Optional.of(Inscricao.builder()
                        .id(UUID.randomUUID())
                        .aluno(Usuario.builder().id(ALUNO_ID).build())
                        .status(InscricaoStatus.CANCELADA)
                        .build()));

        assertThatThrownBy(() -> inscricaoService.inscrever(EVENTO_ID, ALUNO_ID))
                .as("desistir e voltar nao pode ser um atalho para furar a fila")
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("inscricao ativa duplicada continua sendo conflito")
    void naoDuplicaInscricaoAtiva() {
        eventoCom(400, 10);
        when(inscricaoRepository.findByAlunoIdAndEventoId(ALUNO_ID, EVENTO_ID))
                .thenReturn(Optional.of(Inscricao.builder()
                        .id(UUID.randomUUID())
                        .aluno(Usuario.builder().id(ALUNO_ID).build())
                        .status(InscricaoStatus.ATIVA)
                        .build()));

        assertThatThrownBy(() -> inscricaoService.inscrever(EVENTO_ID, ALUNO_ID))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("ja possui inscricao");
    }

    @Test
    @DisplayName("nao deixa se inscrever em evento que ja terminou")
    void recusaEventoEncerrado() {
        Evento encerrado = Evento.builder()
                .id(EVENTO_ID)
                .titulo("Palestra de ontem")
                .dataHoraInicio(LocalDateTime.now().minusDays(1).minusHours(2))
                .dataHoraFim(LocalDateTime.now().minusDays(1))
                .pontos(100)
                .build();
        when(eventoRepository.travarParaInscricao(EVENTO_ID.toString())).thenReturn(Optional.of(encerrado));

        assertThatThrownBy(() -> inscricaoService.inscrever(EVENTO_ID, ALUNO_ID))
                .as("sem essa trava o aluno se inscreve num evento passado e leva no-show na hora, "
                        + "por um evento que nunca teve chance de assistir")
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja terminou");

        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("a linha do evento e travada antes de contar as vagas")
    void usaTravaDeConcorrencia() {
        eventoCom(400, 10);

        inscricaoService.inscrever(EVENTO_ID, ALUNO_ID);

        // findById comum permitiria dois alunos contarem a mesma ultima
        // vaga ao mesmo tempo e ambos entrarem.
        verify(eventoRepository).travarParaInscricao(EVENTO_ID.toString());
        verify(eventoRepository, never()).findById(any());
    }
}
