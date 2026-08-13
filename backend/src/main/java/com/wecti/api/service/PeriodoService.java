package com.wecti.api.service;

import com.wecti.api.domain.Periodo;
import com.wecti.api.dto.NovoPeriodoRequest;
import com.wecti.api.exception.CampoInvalidoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.repository.PeriodoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PeriodoService {

    private final PeriodoRepository periodoRepository;

    public PeriodoService(PeriodoRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    public List<Periodo> listar() {
        return periodoRepository.findAll();
    }

    public Periodo buscarPorId(UUID id) {
        return periodoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Periodo nao encontrado: " + id));
    }

    public Periodo criar(NovoPeriodoRequest request) {
        if (!request.dataFim().isAfter(request.dataInicio())) {
            throw new CampoInvalidoException("dataFim", "A data de fim deve ser depois da data de inicio");
        }
        Periodo periodo = Periodo.builder()
                .nome(request.nome())
                .dataInicio(request.dataInicio())
                .dataFim(request.dataFim())
                .build();
        return periodoRepository.save(periodo);
    }
}
