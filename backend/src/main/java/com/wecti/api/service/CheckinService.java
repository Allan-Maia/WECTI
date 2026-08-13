package com.wecti.api.service;

import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.domain.InscricaoStatus;
import com.wecti.api.exception.ConflitoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.CheckinRepository;
import com.wecti.api.repository.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CheckinService {

    private final CheckinRepository checkinRepository;
    private final InscricaoRepository inscricaoRepository;

    public CheckinService(CheckinRepository checkinRepository, InscricaoRepository inscricaoRepository) {
        this.checkinRepository = checkinRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public Checkin registrarEntrada(String qrcodeToken) {
        Inscricao inscricao = inscricaoRepository.findByQrcodeToken(qrcodeToken)
                .filter(i -> i.getStatus() == InscricaoStatus.ATIVA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("QR code invalido ou inscricao nao encontrada"));

        if (checkinRepository.findByInscricaoId(inscricao.getId()).isPresent()) {
            throw new ConflitoException("Check-in ja realizado para esta inscricao");
        }

        Checkin checkin = Checkin.builder()
                .inscricao(inscricao)
                .entrada(LocalDateTime.now())
                .build();
        return checkinRepository.save(checkin);
    }

    public Checkin registrarSaida(UUID checkinId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Checkin nao encontrado: " + checkinId));

        if (checkin.getSaida() != null) {
            throw new RegraNegocioException("Check-out ja registrado para este check-in");
        }

        checkin.setSaida(LocalDateTime.now());
        return checkinRepository.save(checkin);
    }
}
