package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.exception.ConflitoException;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.CheckinRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registra entrada/saida a partir de uma Inscricao ja identificada (pelo
 * aluno autenticado + evento do QR code escaneado) - ver
 * CheckinSessaoService, que resolve qual Inscricao antes de chamar aqui.
 */
@Service
public class CheckinService {

    private final CheckinRepository checkinRepository;

    public CheckinService(CheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }

    public Checkin registrarEntrada(Inscricao inscricao) {
        if (checkinRepository.findByInscricaoId(inscricao.getId()).isPresent()) {
            throw new ConflitoException("Check-in ja realizado para esta inscricao");
        }

        Checkin checkin = Checkin.builder()
                .inscricao(inscricao)
                .entrada(LocalDateTime.now())
                .build();
        try {
            return checkinRepository.save(checkin);
        } catch (DataIntegrityViolationException e) {
            // A checagem acima (findByInscricaoId) nao e atomica com o save -
            // duas confirmacoes praticamente simultaneas (ex.: o aluno
            // escaneando o QR duas vezes seguidas, ou um retry de rede) podem
            // passar pela checagem juntas. A constraint UNIQUE de
            // checkins.inscricao_id pega essa corrida; convertemos aqui pra
            // um 409 amigavel em vez de deixar estourar como erro 500.
            throw new ConflitoException("Check-in ja realizado para esta inscricao");
        }
    }

    public Checkin registrarSaida(Inscricao inscricao) {
        Checkin checkin = checkinRepository.findByInscricaoId(inscricao.getId())
                .orElseThrow(() -> new RegraNegocioException("Voce ainda nao fez check-in nesta palestra"));

        if (checkin.getSaida() != null) {
            throw new RegraNegocioException("Check-out ja registrado para este check-in");
        }

        checkin.setSaida(LocalDateTime.now());
        return checkinRepository.save(checkin);
    }

    /** Relatório de presença (AdminCheckinPage - "Participantes do
     *  Evento") - quem já fez check-in nesse evento, com ou sem check-out
     *  ainda. A existência do evento é validada por quem chama (ver
     *  CheckinSessaoController). */
    public List<Checkin> listarPorEvento(UUID eventoId) {
        return checkinRepository.findByInscricao_Evento_IdOrderByEntradaAsc(eventoId);
    }
}
