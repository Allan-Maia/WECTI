package com.wecti.api.service;

import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.PontuacaoExtra;
import com.wecti.api.dto.EventoPontuacaoItemResponse;
import com.wecti.api.dto.PontuacaoExtraResponse;
import com.wecti.api.dto.PontuacaoResponse;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pontuacao e calculada dinamicamente a partir de Inscricao/Checkin/Evento
 * a cada consulta (nao ha tabela de "saldo" persistida) - por isso o
 * resultado ja reflete no-shows de eventos encerrados mesmo antes do job
 * agendado (NoShowSchedulerJob) rodar; o job serve apenas para registrar
 * a penalizacao no log no momento em que o evento termina.
 *
 * <p>O total soma duas parcelas: os eventos (regra em
 * {@link CalculoPontuacaoEvento}) e os pontos extras lancados pelo admin
 * ({@link PontuacaoExtraService}). As duas aparecem separadas na resposta
 * porque o aluno precisa conseguir explicar o proprio numero.
 *
 * <p><b>Sem recorte por semestre.</b> A pontuacao e do aluno no WECTI,
 * ponto. O conceito de "periodo" foi removido: nunca foi validado com o
 * professor ("nao precisa relacionar com nada"), e obrigava toda consulta
 * a encontrar um periodo que contivesse a data de hoje - o que fazia a
 * tela de pontuacao parar de carregar assim que o semestre cadastrado
 * terminava.
 */
@Service
public class PontuacaoService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final CheckinRepository checkinRepository;
    private final PontuacaoExtraService pontuacaoExtraService;

    public PontuacaoService(EventoRepository eventoRepository, InscricaoRepository inscricaoRepository,
                             CheckinRepository checkinRepository, PontuacaoExtraService pontuacaoExtraService) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.checkinRepository = checkinRepository;
        this.pontuacaoExtraService = pontuacaoExtraService;
    }

    public PontuacaoResponse calcular(UUID alunoId) {
        List<Evento> eventos = eventoRepository.findAll();
        List<EventoPontuacaoItemResponse> itens = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        int pontosEventos = 0;

        for (Evento evento : eventos) {
            var inscricaoOpt = inscricaoRepository.findByAlunoIdAndEventoId(alunoId, evento.getId());
            if (inscricaoOpt.isEmpty()) {
                continue;
            }
            Inscricao inscricao = inscricaoOpt.get();
            var checkin = checkinRepository.findByInscricaoId(inscricao.getId()).orElse(null);

            var resultado = CalculoPontuacaoEvento.avaliar(evento, inscricao, checkin, agora);
            if (resultado == null) {
                continue;
            }
            itens.add(new EventoPontuacaoItemResponse(evento.getId(), evento.getTitulo(),
                    resultado.pontos(), resultado.status()));
            pontosEventos += resultado.pontos();
        }

        List<PontuacaoExtra> extras = pontuacaoExtraService.listar(alunoId);
        int pontosExtras = extras.stream().mapToInt(PontuacaoExtra::getPontos).sum();

        return new PontuacaoResponse(
                pontosEventos + pontosExtras,
                pontosEventos,
                pontosExtras,
                itens,
                extras.stream().map(PontuacaoExtraResponse::de).toList());
    }
}
