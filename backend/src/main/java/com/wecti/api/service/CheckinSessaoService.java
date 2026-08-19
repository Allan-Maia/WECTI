package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.SessaoCheckin;
import com.wecti.api.domain.TipoSessaoCheckin;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.SessaoCheckinRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * QR de check-in/check-out por EVENTO (nao mais por aluno) - admin ou
 * professor gera e projeta na tela, o proprio aluno escaneia com o
 * celular (camera nativa, sem precisar de app) e confirma a propria
 * presenca. Ver docs/openapi.yaml para o fluxo completo.
 */
@Service
public class CheckinSessaoService {

    private static final long VALIDADE_HORAS = 6;

    private final SessaoCheckinRepository sessaoRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final CheckinService checkinService;

    public CheckinSessaoService(SessaoCheckinRepository sessaoRepository, EventoRepository eventoRepository,
                                 InscricaoRepository inscricaoRepository, CheckinService checkinService) {
        this.sessaoRepository = sessaoRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.checkinService = checkinService;
    }

    public SessaoCheckin criar(UUID eventoId, TipoSessaoCheckin tipo) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento nao encontrado: " + eventoId));

        LocalDateTime agora = LocalDateTime.now();
        SessaoCheckin sessao = SessaoCheckin.builder()
                .evento(evento)
                .tipo(tipo)
                .criadaEm(agora)
                .expiraEm(agora.plusHours(VALIDADE_HORAS))
                .build();
        return sessaoRepository.save(sessao);
    }

    public SessaoCheckin buscarValida(UUID sessaoId) {
        SessaoCheckin sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("QR code invalido"));
        if (sessao.isExpirada()) {
            throw new RegraNegocioException("Este QR code ja expirou - peca pro professor gerar um novo");
        }
        return sessao;
    }

    public Checkin confirmar(UUID sessaoId, UUID alunoId) {
        SessaoCheckin sessao = buscarValida(sessaoId);

        Inscricao inscricao = inscricaoRepository.findByAlunoIdAndEventoId(alunoId, sessao.getEvento().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Voce nao esta inscrito neste evento"));

        if (inscricao.getStatus() == InscricaoStatus.CANCELADA) {
            throw new RegraNegocioException("Sua inscricao neste evento esta cancelada");
        }

        return sessao.getTipo() == TipoSessaoCheckin.ENTRADA
                ? checkinService.registrarEntrada(inscricao)
                : checkinService.registrarSaida(inscricao);
    }
}
