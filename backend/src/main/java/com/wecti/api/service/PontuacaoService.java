package com.wecti.api.service;

import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.Periodo;
import com.wecti.api.dto.EventoPontuacaoItemResponse;
import com.wecti.api.dto.PontuacaoPeriodoResponse;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.PeriodoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
 */
@Service
public class PontuacaoService {

    private final PeriodoRepository periodoRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final CheckinRepository checkinRepository;

    public PontuacaoService(PeriodoRepository periodoRepository, EventoRepository eventoRepository,
                             InscricaoRepository inscricaoRepository, CheckinRepository checkinRepository) {
        this.periodoRepository = periodoRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.checkinRepository = checkinRepository;
    }

    public PontuacaoPeriodoResponse calcular(UUID alunoId, UUID periodoId) {
        Periodo periodo = periodoId != null ? buscarPeriodo(periodoId) : periodoAtual();

        List<Evento> eventos = eventoRepository.findByPeriodoId(periodo.getId());
        List<EventoPontuacaoItemResponse> itens = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        int total = 0;

        for (Evento evento : eventos) {
            var inscricaoOpt = inscricaoRepository.findByAlunoIdAndEventoId(alunoId, evento.getId());
            if (inscricaoOpt.isEmpty()) {
                continue;
            }
            Inscricao inscricao = inscricaoOpt.get();

            if (inscricao.getStatus() == InscricaoStatus.CANCELADA) {
                itens.add(new EventoPontuacaoItemResponse(evento.getId(), evento.getTitulo(), 0, "cancelado"));
                continue;
            }

            if (evento.getDataHoraFim().isAfter(agora)) {
                continue;
            }

            var checkin = checkinRepository.findByInscricaoId(inscricao.getId());
            if (checkin.isEmpty()) {
                itens.add(new EventoPontuacaoItemResponse(evento.getId(), evento.getTitulo(),
                        -evento.getPontos(), "no_show"));
                total -= evento.getPontos();
            } else if (checkin.get().isPresencaQualificada(evento.getDataHoraInicio(), evento.getDataHoraFim())) {
                itens.add(new EventoPontuacaoItemResponse(evento.getId(), evento.getTitulo(),
                        evento.getPontos(), "concluido"));
                total += evento.getPontos();
            } else {
                itens.add(new EventoPontuacaoItemResponse(evento.getId(), evento.getTitulo(), 0, "concluido"));
            }
        }

        return new PontuacaoPeriodoResponse(periodo.getId(), periodo.getNome(), total, itens);
    }

    private Periodo buscarPeriodo(UUID periodoId) {
        return periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Periodo nao encontrado: " + periodoId));
    }

    private Periodo periodoAtual() {
        LocalDate hoje = LocalDate.now();
        return periodoRepository.findAll().stream()
                .filter(p -> !hoje.isBefore(p.getDataInicio()) && !hoje.isAfter(p.getDataFim()))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum periodo ativo no momento"));
    }
}
