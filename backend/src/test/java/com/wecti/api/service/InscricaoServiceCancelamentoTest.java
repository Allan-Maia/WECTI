package com.wecti.api.service;

import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.Usuario;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
 * Cancelamento de inscricao: permitido ate 1 dia antes do inicio do
 * evento. Passado esse prazo a inscricao continua valendo e, se o aluno
 * nao aparecer, vira no-show com perda de pontos - por isso o limite
 * precisa estar certo nos dois sentidos.
 */
@ExtendWith(MockitoExtension.class)
class InscricaoServiceCancelamentoTest {

    private static final UUID ALUNO_ID = UUID.randomUUID();

    @Mock private InscricaoRepository inscricaoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CheckinRepository checkinRepository;

    @InjectMocks private InscricaoService inscricaoService;

    private Inscricao inscricaoParaEventoEm(LocalDateTime inicioDoEvento, InscricaoStatus status) {
        Evento evento = Evento.builder()
                .id(UUID.randomUUID())
                .titulo("Palestra")
                .dataHoraInicio(inicioDoEvento)
                .dataHoraFim(inicioDoEvento.plusHours(2))
                .pontos(100)
                .build();

        Inscricao inscricao = Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(Usuario.builder().id(ALUNO_ID).build())
                .evento(evento)
                .status(status)
                .build();

        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
        return inscricao;
    }

    @Test
    @DisplayName("cancela com folga: evento daqui a 5 dias")
    void cancelaComFolga() {
        Inscricao inscricao = inscricaoParaEventoEm(LocalDateTime.now().plusDays(5), InscricaoStatus.ATIVA);

        inscricaoService.cancelar(inscricao.getId(), ALUNO_ID);

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.CANCELADA);
        assertThat(inscricao.getCanceladaEm()).isNotNull();
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    @DisplayName("cancela no limite: falta pouco mais de 1 dia")
    void cancelaNoLimite() {
        Inscricao inscricao = inscricaoParaEventoEm(
                LocalDateTime.now().plusDays(1).plusMinutes(5), InscricaoStatus.ATIVA);

        inscricaoService.cancelar(inscricao.getId(), ALUNO_ID);

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.CANCELADA);
    }

    @Test
    @DisplayName("recusa dentro das 24h: prazo ja passou")
    void recusaDentroDoPrazo() {
        Inscricao inscricao = inscricaoParaEventoEm(
                LocalDateTime.now().plusHours(20), InscricaoStatus.ATIVA);

        assertThatThrownBy(() -> inscricaoService.cancelar(inscricao.getId(), ALUNO_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Prazo de cancelamento");

        assertThat(inscricao.getStatus())
                .as("a inscricao precisa continuar ATIVA - senao o no-show nao seria aplicado")
                .isEqualTo(InscricaoStatus.ATIVA);
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("recusa depois do evento ja ter comecado")
    void recusaEventoJaComecado() {
        Inscricao inscricao = inscricaoParaEventoEm(
                LocalDateTime.now().minusHours(1), InscricaoStatus.ATIVA);

        assertThatThrownBy(() -> inscricaoService.cancelar(inscricao.getId(), ALUNO_ID))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("recusa cancelar duas vezes")
    void recusaCancelarDuasVezes() {
        Inscricao inscricao = inscricaoParaEventoEm(
                LocalDateTime.now().plusDays(5), InscricaoStatus.CANCELADA);

        assertThatThrownBy(() -> inscricaoService.cancelar(inscricao.getId(), ALUNO_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta cancelada");
    }

    @Test
    @DisplayName("um aluno nao cancela a inscricao de outro")
    void recusaCancelarInscricaoDeOutro() {
        Inscricao inscricao = inscricaoParaEventoEm(
                LocalDateTime.now().plusDays(5), InscricaoStatus.ATIVA);
        UUID outroAluno = UUID.randomUUID();

        assertThatThrownBy(() -> inscricaoService.cancelar(inscricao.getId(), outroAluno))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(inscricao.getStatus()).isEqualTo(InscricaoStatus.ATIVA);
        verify(inscricaoRepository, never()).save(any());
    }
}
