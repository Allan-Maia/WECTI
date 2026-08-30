package com.wecti.api.service;

import com.wecti.api.domain.Periodo;
import com.wecti.api.dto.NovoPeriodoRequest;
import com.wecti.api.exception.CampoInvalidoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.repository.PeriodoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    /**
     * O periodo pedido, ou o vigente quando nenhum e informado.
     *
     * <p>Fica aqui, e nao repetido em cada servico, porque pontuacao,
     * ranking e pontos extras precisam concordar sobre qual e "o periodo
     * atual" - se cada um decidisse por conta, um aluno poderia ver seus
     * pontos num semestre e aparecer no ranking de outro.
     */
    public Periodo resolver(UUID periodoId) {
        return periodoId != null ? buscarPorId(periodoId) : periodoVigente();
    }

    public Periodo periodoVigente() {
        LocalDate hoje = LocalDate.now();
        return periodoRepository.findQueContem(hoje).stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhum periodo ativo no momento"));
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
