package com.wecti.api.service;

import com.wecti.api.domain.Evento;
import com.wecti.api.dto.EventoResponse;
import com.wecti.api.dto.NovoEventoRequest;
import com.wecti.api.exception.CampoInvalidoException;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.EventoRepository;
import com.wecti.api.repository.InscricaoRepository;
import com.wecti.api.repository.PalestranteRepository;
import com.wecti.api.repository.PeriodoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final PeriodoRepository periodoRepository;
    private final PalestranteRepository palestranteRepository;
    private final InscricaoRepository inscricaoRepository;
    private final InscricaoService inscricaoService;

    public EventoService(EventoRepository eventoRepository, PeriodoRepository periodoRepository,
                          PalestranteRepository palestranteRepository, InscricaoRepository inscricaoRepository,
                          InscricaoService inscricaoService) {
        this.eventoRepository = eventoRepository;
        this.periodoRepository = periodoRepository;
        this.palestranteRepository = palestranteRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.inscricaoService = inscricaoService;
    }

    public List<Evento> listar(UUID periodoId, String status) {
        List<Evento> eventos = periodoId != null
                ? eventoRepository.findByPeriodoId(periodoId)
                : eventoRepository.findAll();

        LocalDateTime agora = LocalDateTime.now();
        if ("futuros".equals(status)) {
            return eventos.stream().filter(e -> e.getDataHoraInicio().isAfter(agora)).toList();
        }
        if ("encerrados".equals(status)) {
            return eventos.stream().filter(e -> e.getDataHoraFim().isBefore(agora)).toList();
        }
        return eventos;
    }

    public Evento buscarPorId(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento nao encontrado: " + id));
    }

    /**
     * Lista os eventos ja com a ocupacao de vagas preenchida. A contagem
     * sai de uma consulta agrupada unica - contar evento a evento faria
     * uma consulta por linha da tela.
     */
    public List<EventoResponse> listarComVagas(UUID periodoId, String status) {
        List<Evento> eventos = listar(periodoId, status);
        Map<UUID, Long> ocupacao = inscricaoService.inscritosAtivosPorEvento(
                eventos.stream().map(Evento::getId).toList());
        return eventos.stream()
                .map(e -> EventoResponse.de(e, ocupacao.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public EventoResponse detalharComVagas(UUID id) {
        Evento evento = buscarPorId(id);
        return EventoResponse.de(evento, inscricaoService.inscritosAtivos(id));
    }

    public EventoResponse criar(NovoEventoRequest request) {
        validarDatas(request);
        Evento evento = Evento.builder()
                .periodo(buscarPeriodoPelaData(request.dataHoraInicio()))
                .titulo(request.titulo())
                .descricao(request.descricao())
                .local(request.local())
                .dataHoraInicio(request.dataHoraInicio())
                .dataHoraFim(request.dataHoraFim())
                .pontos(request.pontos())
                .capacidade(request.capacidade())
                .palestrantes(buscarPalestrantes(request.palestranteIds()))
                .build();
        return EventoResponse.de(eventoRepository.save(evento), 0L);
    }

    public EventoResponse atualizar(UUID id, NovoEventoRequest request) {
        validarDatas(request);
        Evento evento = buscarPorId(id);
        long inscritos = inscricaoService.inscritosAtivos(id);
        validarCapacidade(request.capacidade(), inscritos);

        evento.setPeriodo(buscarPeriodoPelaData(request.dataHoraInicio()));
        evento.setTitulo(request.titulo());
        evento.setDescricao(request.descricao());
        evento.setLocal(request.local());
        evento.setDataHoraInicio(request.dataHoraInicio());
        evento.setDataHoraFim(request.dataHoraFim());
        evento.setPontos(request.pontos());
        evento.setCapacidade(request.capacidade());
        evento.setPalestrantes(buscarPalestrantes(request.palestranteIds()));
        return EventoResponse.de(eventoRepository.save(evento), inscritos);
    }

    /**
     * Reduzir a capacidade abaixo de quem ja esta inscrito deixaria o
     * evento num estado impossivel de resolver pelo sistema: o admin
     * teria que escolher a mao quem perde a vaga, e nao ha tela para
     * isso. Melhor recusar e deixar claro quantos ja entraram.
     */
    private void validarCapacidade(Integer capacidade, long inscritos) {
        if (capacidade != null && capacidade < inscritos) {
            throw new RegraNegocioException(
                    "Este evento ja tem " + inscritos + " inscrito(s) - a capacidade nao pode ser menor que isso.");
        }
    }

    public void cancelar(UUID id) {
        Evento evento = buscarPorId(id);
        if (!inscricaoRepository.findByEventoId(id).isEmpty()) {
            throw new RegraNegocioException("Nao e possivel cancelar um evento que ja possui inscricoes");
        }
        eventoRepository.delete(evento);
    }

    private void validarDatas(NovoEventoRequest request) {
        if (!request.dataHoraFim().isAfter(request.dataHoraInicio())) {
            throw new CampoInvalidoException("dataHoraFim", "A data/hora de termino deve ser depois da data/hora de inicio");
        }
    }

    /**
     * O admin nao escolhe mais o Periodo manualmente (o formulario ja tem
     * data/hora do evento, que basta) - descobrimos sozinhos em qual
     * Periodo cadastrado a data do evento cai. Se nenhum Periodo cobrir
     * essa data, quem esta cadastrando o evento precisa cadastrar um
     * Periodo pra ela antes (por API - POST /periodos).
     */
    private com.wecti.api.domain.Periodo buscarPeriodoPelaData(LocalDateTime dataHoraInicio) {
        List<com.wecti.api.domain.Periodo> periodos = periodoRepository.findQueContem(dataHoraInicio.toLocalDate());
        if (periodos.isEmpty()) {
            throw new RegraNegocioException(
                    "Nao existe um Periodo cadastrado que cubra a data do evento (" + dataHoraInicio.toLocalDate()
                            + "). Cadastre um Periodo com esse intervalo antes de criar o evento.");
        }
        return periodos.get(0);
    }

    private Set<com.wecti.api.domain.Palestrante> buscarPalestrantes(List<UUID> palestranteIds) {
        if (palestranteIds == null || palestranteIds.isEmpty()) {
            return new HashSet<>();
        }
        // IDs unicos antes de comparar tamanho - senao uma lista com ID
        // repetido (ex.: [x, x]) disparava falso "nao existe", ja que
        // findAllById devolve so uma linha por ID unico.
        Set<UUID> idsUnicos = new HashSet<>(palestranteIds);
        List<com.wecti.api.domain.Palestrante> encontrados = palestranteRepository.findAllById(idsUnicos);
        if (encontrados.size() != idsUnicos.size()) {
            throw new RecursoNaoEncontradoException("Um ou mais palestrantes informados nao existem");
        }
        return new HashSet<>(encontrados);
    }
}
