package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.domain.Usuario;
import com.wecti.api.dto.CheckinResponse;
import com.wecti.api.dto.InscricaoResponse;
import com.wecti.api.exception.ConflitoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CheckinRepository checkinRepository;
    private final EmailService emailService;

    public InscricaoService(InscricaoRepository inscricaoRepository, EventoRepository eventoRepository,
                             UsuarioRepository usuarioRepository, CheckinRepository checkinRepository,
                             EmailService emailService) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.checkinRepository = checkinRepository;
        this.emailService = emailService;
    }

    public Inscricao inscrever(UUID eventoId, UUID alunoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento nao encontrado: " + eventoId));
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + alunoId));

        if (inscricaoRepository.existsByAlunoIdAndEventoId(alunoId, eventoId)) {
            throw new ConflitoException("Aluno ja possui inscricao para este evento");
        }

        Inscricao inscricao = Inscricao.builder()
                .aluno(aluno)
                .evento(evento)
                .status(InscricaoStatus.ATIVA)
                .qrcodeToken(UUID.randomUUID().toString())
                .build();
        inscricao = inscricaoRepository.save(inscricao);

        emailService.enviarQrCode(inscricao);
        return inscricao;
    }

    public void cancelar(UUID inscricaoId, UUID alunoId) {
        Inscricao inscricao = buscarEntidade(inscricaoId);
        exigirDono(inscricao, alunoId);

        if (inscricao.getStatus() == InscricaoStatus.CANCELADA) {
            throw new RegraNegocioException("Inscricao ja esta cancelada");
        }

        LocalDateTime prazoLimite = inscricao.getEvento().getDataHoraInicio().minusDays(1);
        if (!LocalDateTime.now().isBefore(prazoLimite)) {
            throw new RegraNegocioException(
                    "Prazo de cancelamento (1 dia antes do evento) ja passou");
        }

        inscricao.setStatus(InscricaoStatus.CANCELADA);
        inscricao.setCanceladaEm(LocalDateTime.now());
        inscricaoRepository.save(inscricao);
    }

    public Inscricao buscarEntidade(UUID inscricaoId) {
        return inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Inscricao nao encontrada: " + inscricaoId));
    }

    public Inscricao buscarComPermissao(UUID inscricaoId, UUID alunoIdSeAluno) {
        Inscricao inscricao = buscarEntidade(inscricaoId);
        if (alunoIdSeAluno != null) {
            exigirDono(inscricao, alunoIdSeAluno);
        }
        return inscricao;
    }

    public List<Inscricao> listarPorEvento(UUID eventoId) {
        return inscricaoRepository.findByEventoId(eventoId);
    }

    public List<Inscricao> listarDoAluno(UUID alunoId, String status) {
        List<Inscricao> inscricoes = inscricaoRepository.findByAlunoId(alunoId);
        LocalDateTime agora = LocalDateTime.now();
        if ("futuros".equals(status)) {
            return inscricoes.stream()
                    .filter(i -> i.getStatus() == InscricaoStatus.ATIVA
                            && i.getEvento().getDataHoraInicio().isAfter(agora))
                    .toList();
        }
        if ("historico".equals(status)) {
            return inscricoes.stream()
                    .filter(i -> i.getStatus() == InscricaoStatus.CANCELADA
                            || !i.getEvento().getDataHoraInicio().isAfter(agora))
                    .toList();
        }
        return inscricoes;
    }

    public InscricaoResponse montarResposta(Inscricao inscricao) {
        Optional<Checkin> checkin = checkinRepository.findByInscricaoId(inscricao.getId());
        CheckinResponse checkinResponse = checkin.map(c -> CheckinResponse.de(c, inscricao.getEvento())).orElse(null);
        boolean certificadoDisponivel = checkin
                .map(c -> c.isPresencaQualificada(inscricao.getEvento().getDataHoraInicio(),
                        inscricao.getEvento().getDataHoraFim()))
                .orElse(false);
        return InscricaoResponse.de(inscricao, checkinResponse, certificadoDisponivel);
    }

    private void exigirDono(Inscricao inscricao, UUID alunoId) {
        if (!inscricao.getAluno().getId().equals(alunoId)) {
            throw new AccessDeniedException("Inscricao pertence a outro aluno");
        }
    }
}
