package com.wecti.api.service;

import com.wecti.api.domain.Evento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Ate quando da para se inscrever num evento - e, pelo mesmo prazo,
 * cancelar.
 *
 * <p><b>A regra.</b> Vale ate a hora de inicio do evento, mais uma folga
 * (padrao: 15 minutos). A folga existe para quem chega atrasado: sem ela,
 * o aluno que aparece 5 minutos depois do comeco nao consegue se
 * inscrever, e portanto nao consegue fazer check-in nem pontuar - ficaria
 * de fora de uma palestra em que esta fisicamente presente.
 *
 * <p><b>Por que inscricao e cancelamento fecham juntos.</b> Enquanto da
 * para entrar, tem que dar para sair: quem clicou por engano, ou desistiu
 * no ultimo minuto, devolve a vaga para outro. Depois que o prazo fecha,
 * quem ficou inscrito e nao apareceu leva no-show - e e isso que faz a
 * penalidade ter sentido.
 *
 * <p><b>A folga cabe na janela de check-in.</b> O check-in abre 30
 * minutos antes do inicio (ver CheckinSessaoService), entao quem se
 * inscreve dentro da folga ainda consegue confirmar presenca. Se algum
 * dia a folga passar dessa margem, o aluno conseguiria se inscrever num
 * evento em que nao pode mais fazer check-in.
 *
 * <p>Mora aqui, e nao na entidade Evento, porque agora depende de
 * configuracao - e num lugar so, porque tres pontos precisam concordar:
 * a inscricao, o cancelamento e o que a tela mostra.
 */
@Service
public class PrazoInscricao {

    private final long toleranciaMinutos;

    public PrazoInscricao(
            @Value("${app.inscricao.tolerancia-apos-inicio-minutos}") long toleranciaMinutos) {
        this.toleranciaMinutos = toleranciaMinutos;
    }

    /** Instante em que as inscricoes (e os cancelamentos) fecham. */
    public LocalDateTime fechamento(Evento evento) {
        return evento.getDataHoraInicio().plusMinutes(toleranciaMinutos);
    }

    public boolean estaAberto(Evento evento) {
        return LocalDateTime.now().isBefore(fechamento(evento));
    }
}
