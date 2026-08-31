package com.wecti.api.service;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.PontuacaoExtra;
import com.wecti.api.domain.Usuario;
import com.wecti.api.dto.NovaPontuacaoExtraRequest;
import com.wecti.api.exception.CampoInvalidoException;
import com.wecti.api.repository.PontuacaoExtraRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lancamento manual de pontos (gincanas).
 *
 * <p>E a unica entrada de pontos que nao vem de uma regra automatica: o
 * numero e digitado a mao, no meio de um evento, e vai decidir premiacao.
 * As travas testadas aqui existem para que um erro de digitacao nao
 * apareca so na hora de premiar.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PontuacaoExtraServiceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock private PontuacaoExtraRepository repository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PontuacaoExtraService service;

    private Usuario aluno;

    @BeforeEach
    void preparar() {
        aluno = Usuario.builder().id(UUID.randomUUID()).nome("Ana").perfil(Perfil.ALUNO).rgm("12345678").build();
        when(usuarioRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(
                Usuario.builder().id(ADMIN_ID).nome("Admin").perfil(Perfil.ADMIN).build()));
        when(repository.save(any(PontuacaoExtra.class))).thenAnswer(i -> i.getArgument(0));
    }

    private NovaPontuacaoExtraRequest pedido(int pontos, String motivo) {
        return new NovaPontuacaoExtraRequest(aluno.getId(), pontos, motivo);
    }

    @Test
    @DisplayName("lanca pontos guardando quem lancou e por que")
    void lancaComAutoria() {
        var extra = service.lancar(pedido(50, "1o lugar na gincana de logica"), ADMIN_ID);

        assertThat(extra.getPontos()).isEqualTo(50);
        assertThat(extra.getMotivo()).isEqualTo("1o lugar na gincana de logica");
        assertThat(extra.getAluno()).isEqualTo(aluno);
        assertThat(extra.getCriadoPor().getId())
                .as("ranking contestado sem autoria do lancamento e impossivel de auditar")
                .isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("aceita valor negativo para corrigir um lancamento a maior")
    void aceitaNegativoParaCorrecao() {
        assertThat(service.lancar(pedido(-50, "Estorno do lancamento em duplicidade"), ADMIN_ID).getPontos())
                .isEqualTo(-50);
    }

    @Test
    @DisplayName("recusa lancamento de zero pontos")
    void recusaZero() {
        assertThatThrownBy(() -> service.lancar(pedido(0, "Nada"), ADMIN_ID))
                .isInstanceOf(CampoInvalidoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recusa valor absurdo - trava contra dedo escorregado")
    void recusaValorAcimaDoLimite() {
        assertThatThrownBy(() -> service.lancar(pedido(5000, "Gincana"), ADMIN_ID))
                .as("digitar 5000 em vez de 50 decidiria o ranking inteiro sem ninguem perceber")
                .isInstanceOf(CampoInvalidoException.class)
                .hasMessageContaining("1000");

        assertThatThrownBy(() -> service.lancar(pedido(-5000, "Gincana"), ADMIN_ID))
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    @DisplayName("aceita exatamente o limite")
    void aceitaOLimite() {
        assertThat(service.lancar(pedido(1000, "Premio maximo"), ADMIN_ID).getPontos()).isEqualTo(1000);
    }

    @Test
    @DisplayName("nao lanca pontos para quem nao e aluno")
    void recusaNaoAluno() {
        Usuario outroAdmin = Usuario.builder().id(UUID.randomUUID()).nome("Admin 2").perfil(Perfil.ADMIN).build();
        when(usuarioRepository.findById(outroAdmin.getId())).thenReturn(Optional.of(outroAdmin));

        assertThatThrownBy(() -> service.lancar(
                new NovaPontuacaoExtraRequest(outroAdmin.getId(), 50, "Gincana"), ADMIN_ID))
                .as("admin nao disputa ranking - lancar pontos nele so criaria confusao na apuracao")
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    @DisplayName("soma os lancamentos por aluno para o ranking")
    void somaPorAluno() {
        UUID outroAluno = UUID.randomUUID();
        when(repository.findTodosParaRanking()).thenReturn(List.of(
                extraDe(aluno.getId(), 50),
                extraDe(aluno.getId(), 30),
                extraDe(aluno.getId(), -10),
                extraDe(outroAluno, 100)));

        var totais = service.totaisPorAluno();

        assertThat(totais).containsEntry(aluno.getId(), 70).containsEntry(outroAluno, 100);
    }

    private PontuacaoExtra extraDe(UUID alunoId, int pontos) {
        return PontuacaoExtra.builder()
                .id(UUID.randomUUID())
                .aluno(Usuario.builder().id(alunoId).build())
                .pontos(pontos)
                .motivo("Gincana")
                .build();
    }
}
