package com.wecti.api.service;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.Periodo;
import com.wecti.api.domain.PontuacaoExtra;
import com.wecti.api.domain.Usuario;
import com.wecti.api.dto.NovaPontuacaoExtraRequest;
import com.wecti.api.exception.CampoInvalidoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.repository.PontuacaoExtraRepository;
import com.wecti.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pontos lancados a mao pelo admin - premio de gincana, tipicamente.
 *
 * <p>Somam-se a pontuacao das palestras dentro do mesmo periodo; nao a
 * substituem. Ver {@link PontuacaoService} e {@link RankingService}, que
 * consomem estes valores.
 */
@Service
public class PontuacaoExtraService {

    /**
     * Teto por lancamento. Nao e regra de negocio do professor: e trava
     * contra dedo escorregado. Digitar 5000 em vez de 50 no meio de um
     * evento decidiria o ranking inteiro, e o erro so apareceria na
     * premiacao.
     */
    private static final int LIMITE_POR_LANCAMENTO = 1000;

    private final PontuacaoExtraRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoService periodoService;

    public PontuacaoExtraService(PontuacaoExtraRepository repository, UsuarioRepository usuarioRepository,
                                  PeriodoService periodoService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.periodoService = periodoService;
    }

    public PontuacaoExtra lancar(NovaPontuacaoExtraRequest request, UUID adminId) {
        if (request.pontos() == 0) {
            throw new CampoInvalidoException("pontos", "Informe uma pontuacao diferente de zero");
        }
        if (Math.abs(request.pontos()) > LIMITE_POR_LANCAMENTO) {
            throw new CampoInvalidoException("pontos",
                    "Lancamento maximo de " + LIMITE_POR_LANCAMENTO + " pontos por vez");
        }

        Usuario aluno = usuarioRepository.findById(request.alunoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));
        // Pontuacao so faz sentido para aluno: admin nao disputa ranking.
        if (aluno.getPerfil() != Perfil.ALUNO) {
            throw new CampoInvalidoException("alunoId", "So e possivel lancar pontos para alunos");
        }

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
        Periodo periodo = periodoService.resolver(request.periodoId());

        return repository.save(PontuacaoExtra.builder()
                .aluno(aluno)
                .periodo(periodo)
                .pontos(request.pontos())
                .motivo(request.motivo().trim())
                .criadoPor(admin)
                .build());
    }

    public List<PontuacaoExtra> listar(UUID alunoId, UUID periodoId) {
        Periodo periodo = periodoService.resolver(periodoId);
        return repository.findByAlunoIdAndPeriodoIdOrderByCriadoEmDesc(alunoId, periodo.getId());
    }

    /** Desfaz um lancamento. E o caminho para corrigir "lancei no aluno
     *  errado" sem deixar rastro confuso de mais e menos. */
    public void remover(UUID id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Lancamento nao encontrado: " + id);
        }
        repository.deleteById(id);
    }

    public int totalDoAluno(UUID alunoId, UUID periodoId) {
        return repository.findByAlunoIdAndPeriodoIdOrderByCriadoEmDesc(alunoId, periodoId).stream()
                .mapToInt(PontuacaoExtra::getPontos)
                .sum();
    }

    /** Extras de todos os alunos do periodo, somados por aluno - o
     *  ranking precisa da turma inteira sem consultar um a um. */
    public Map<UUID, Integer> totaisDoPeriodo(UUID periodoId) {
        return repository.findByPeriodoId(periodoId).stream()
                .collect(Collectors.groupingBy(e -> e.getAluno().getId(),
                        Collectors.summingInt(PontuacaoExtra::getPontos)));
    }
}
