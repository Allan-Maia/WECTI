package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.Periodo;
import com.wecti.api.domain.Usuario;
import com.wecti.api.dto.RankingItemResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Ranking do periodo.
 *
 * <p>O ponto critico coberto aqui e a <b>concordancia com a tela
 * individual</b>: os dois usam {@link CalculoPontuacaoEvento}, e um aluno
 * que visse um total no proprio perfil e outro no ranking perderia a
 * confianca na competicao inteira. Por isso os cenarios repetem as mesmas
 * situacoes de PontuacaoServiceTest (no-show, cancelado, evento em
 * andamento) e conferem o numero final.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RankingServiceTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private InscricaoRepository inscricaoRepository;
    @Mock private CheckinRepository checkinRepository;
    @Mock private PontuacaoExtraService pontuacaoExtraService;
    @Mock private PeriodoService periodoService;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private RankingService rankingService;

    private Periodo periodo;
    private Evento eventoEncerrado;
    private final List<Inscricao> inscricoes = new ArrayList<>();
    private final List<Checkin> checkins = new ArrayList<>();

    @BeforeEach
    void preparar() {
        periodo = Periodo.builder()
                .id(UUID.randomUUID())
                .nome("2026.2")
                .dataInicio(LocalDate.now().minusMonths(1))
                .dataFim(LocalDate.now().plusMonths(3))
                .build();

        LocalDateTime inicio = LocalDateTime.now().minusDays(2);
        eventoEncerrado = Evento.builder()
                .id(UUID.randomUUID())
                .periodo(periodo)
                .titulo("Palestra encerrada")
                .dataHoraInicio(inicio)
                .dataHoraFim(inicio.plusHours(2))
                .pontos(100)
                .build();

        when(periodoService.resolver(null)).thenReturn(periodo);
        when(eventoRepository.findByPeriodoId(periodo.getId())).thenReturn(List.of(eventoEncerrado));
        when(inscricaoRepository.findByPeriodoId(periodo.getId())).thenReturn(inscricoes);
        when(checkinRepository.findByPeriodoId(periodo.getId())).thenReturn(checkins);
        when(pontuacaoExtraService.totaisDoPeriodo(periodo.getId())).thenReturn(Map.of());
    }

    private Usuario aluno(String nome, String rgm) {
        return Usuario.builder().id(UUID.randomUUID()).nome(nome).rgm(rgm).curso("ADS").build();
    }

    /** Inscreve o aluno no evento encerrado com presenca integral. */
    private Usuario presencaCompleta(String nome, String rgm) {
        Usuario usuario = aluno(nome, rgm);
        Inscricao inscricao = inscrever(usuario, InscricaoStatus.ATIVA);
        checkins.add(Checkin.builder()
                .inscricao(inscricao)
                .entrada(eventoEncerrado.getDataHoraInicio())
                .saida(eventoEncerrado.getDataHoraFim())
                .build());
        return usuario;
    }

    private Inscricao inscrever(Usuario usuario, InscricaoStatus status) {
        Inscricao inscricao = Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(usuario)
                .evento(eventoEncerrado)
                .status(status)
                .build();
        inscricoes.add(inscricao);
        return inscricao;
    }

    @Test
    @DisplayName("ordena do maior para o menor total")
    void ordenaPorPontos() {
        Usuario comPresenca = presencaCompleta("Bruno", "11111111");
        Usuario faltou = aluno("Ana", "22222222");
        inscrever(faltou, InscricaoStatus.ATIVA);

        var ranking = rankingService.montar(null, false);

        assertThat(ranking.itens()).extracting(RankingItemResponse::alunoNome)
                .containsExactly(comPresenca.getNome(), faltou.getNome());
        assertThat(ranking.itens().get(0).pontosTotal()).isEqualTo(100);
        assertThat(ranking.itens().get(1).pontosTotal())
                .as("nao cancelou e nao compareceu = no-show, perde os pontos do evento")
                .isEqualTo(-100);
    }

    @Test
    @DisplayName("empate divide a posicao e a seguinte pula (1, 2, 2, 4)")
    void empateDivideAPosicao() {
        presencaCompleta("Ana", "11111111");
        presencaCompleta("Bruno", "22222222");
        presencaCompleta("Carla", "33333333");
        Usuario ultimo = aluno("Daniel", "44444444");
        inscrever(ultimo, InscricaoStatus.CANCELADA);

        var ranking = rankingService.montar(null, false);

        assertThat(ranking.itens()).extracting(RankingItemResponse::posicao)
                .containsExactly(1, 1, 1, 4);
    }

    @Test
    @DisplayName("soma os pontos de gincana ao total")
    void somaPontosExtras() {
        Usuario aluno = presencaCompleta("Ana", "11111111");
        when(pontuacaoExtraService.totaisDoPeriodo(periodo.getId())).thenReturn(Map.of(aluno.getId(), 50));

        var item = rankingService.montar(null, false).itens().get(0);

        assertThat(item.pontosEventos()).isEqualTo(100);
        assertThat(item.pontosExtras()).isEqualTo(50);
        assertThat(item.pontosTotal()).isEqualTo(150);
    }

    @Test
    @DisplayName("gincana pode virar a disputa")
    void gincanaMudaAOrdem() {
        Usuario semGincana = presencaCompleta("Ana", "11111111");
        Usuario comGincana = aluno("Bruno", "22222222");
        inscrever(comGincana, InscricaoStatus.CANCELADA);
        when(pontuacaoExtraService.totaisDoPeriodo(periodo.getId()))
                .thenReturn(Map.of(comGincana.getId(), 500));

        var ranking = rankingService.montar(null, false);

        assertThat(ranking.itens().get(0).alunoNome()).isEqualTo(comGincana.getNome());
        assertThat(ranking.itens().get(1).alunoNome()).isEqualTo(semGincana.getNome());
    }

    @Test
    @DisplayName("aluno que so tem pontos de gincana, sem inscricao, entra no ranking")
    void alunoSoComGincanaAparece() {
        Usuario premiado = aluno("Fora da lista", "99999999");
        when(pontuacaoExtraService.totaisDoPeriodo(periodo.getId())).thenReturn(Map.of(premiado.getId(), 30));
        when(usuarioRepository.findAllById(List.of(premiado.getId()))).thenReturn(List.of(premiado));

        var ranking = rankingService.montar(null, false);

        assertThat(ranking.itens()).singleElement()
                .as("sumir de um ranking em que voce tem pontos e o pior erro possivel aqui")
                .satisfies(item -> {
                    assertThat(item.alunoNome()).isEqualTo(premiado.getNome());
                    assertThat(item.pontosTotal()).isEqualTo(30);
                });
    }

    @Test
    @DisplayName("RGM so vai para a visao do admin")
    void rgmSoParaAdmin() {
        presencaCompleta("Ana", "12345678");

        assertThat(rankingService.montar(null, true).itens().get(0).alunoRgm()).isEqualTo("12345678");
        assertThat(rankingService.montar(null, false).itens().get(0).alunoRgm())
                .as("o ranking do aluno e aberto para a turma inteira - nao espalha o RGM dos colegas")
                .isNull();
    }

    @Test
    @DisplayName("evento ainda em andamento nao pontua nem penaliza")
    void eventoEmAndamentoNaoConta() {
        Evento emAndamento = Evento.builder()
                .id(UUID.randomUUID())
                .periodo(periodo)
                .titulo("Acontecendo agora")
                .dataHoraInicio(LocalDateTime.now().minusMinutes(30))
                .dataHoraFim(LocalDateTime.now().plusHours(1))
                .pontos(100)
                .build();
        when(eventoRepository.findByPeriodoId(periodo.getId())).thenReturn(List.of(emAndamento));

        Usuario aluno = aluno("Ana", "11111111");
        inscricoes.add(Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(aluno)
                .evento(emAndamento)
                .status(InscricaoStatus.ATIVA)
                .build());

        var item = rankingService.montar(null, false).itens().get(0);

        assertThat(item.pontosTotal())
                .as("o aluno ainda pode aparecer - penalizar antes do fim seria injusto")
                .isZero();
    }

    @Test
    @DisplayName("conta quantas palestras o aluno realmente concluiu")
    void contaEventosConcluidos() {
        presencaCompleta("Ana", "11111111");

        var item = rankingService.montar(null, false).itens().get(0);

        assertThat(item.eventosConcluidos()).isEqualTo(1);
    }

    @Test
    @DisplayName("presenca parcial nao pontua e nao conta como palestra concluida")
    void presencaParcialNaoPontua() {
        Usuario aluno = aluno("Ana", "11111111");
        Inscricao inscricao = inscrever(aluno, InscricaoStatus.ATIVA);
        // Entrou e saiu com 30 minutos num evento de 2h = 25%.
        checkins.add(Checkin.builder()
                .inscricao(inscricao)
                .entrada(eventoEncerrado.getDataHoraInicio())
                .saida(eventoEncerrado.getDataHoraInicio().plusMinutes(30))
                .build());

        var item = rankingService.montar(null, false).itens().get(0);

        assertThat(item.pontosTotal()).isZero();
        assertThat(item.eventosConcluidos()).isZero();
    }

    @Test
    @DisplayName("inscricao cancelada nao pontua nem penaliza")
    void canceladaNaoPenaliza() {
        Usuario aluno = aluno("Ana", "11111111");
        inscrever(aluno, InscricaoStatus.CANCELADA);

        assertThat(rankingService.montar(null, false).itens().get(0).pontosTotal()).isZero();
    }
}
