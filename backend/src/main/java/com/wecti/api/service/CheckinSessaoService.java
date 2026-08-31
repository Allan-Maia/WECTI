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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * QR de check-in/check-out por EVENTO (nao mais por aluno) - o admin gera
 * e projeta na tela, o proprio aluno escaneia com o celular (camera
 * nativa, sem precisar de app) e confirma a propria presenca. Ver
 * docs/openapi.yaml para o fluxo completo.
 *
 * <p>Duas travas contra o QR ser repassado pra quem nao esta na sala:
 *
 * <ol>
 *   <li><b>Codigo rotativo</b> ({@link CodigoRotativoCheckin}) - o link
 *       muda a cada janela de tempo, entao um print mandado no grupo
 *       vence junto com a janela em que foi tirado. E a trava que
 *       realmente pega o compartilhamento.</li>
 *   <li><b>Janela do evento</b> - a sessao so aceita confirmacao entre o
 *       inicio e o fim do proprio evento (com tolerancia). Antes era
 *       "criacao + 6 horas", o que deixava um QR gerado de manha
 *       valendo a tarde inteira.</li>
 * </ol>
 */
@Service
public class CheckinSessaoService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM 'as' HH:mm");

    private final SessaoCheckinRepository sessaoRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final CheckinService checkinService;
    private final CodigoRotativoCheckin codigoRotativo;
    private final long toleranciaAntesMinutos;
    private final long toleranciaDepoisMinutos;

    public CheckinSessaoService(SessaoCheckinRepository sessaoRepository, EventoRepository eventoRepository,
                                 InscricaoRepository inscricaoRepository, CheckinService checkinService,
                                 CodigoRotativoCheckin codigoRotativo,
                                 @Value("${app.checkin.tolerancia-antes-minutos}") long toleranciaAntesMinutos,
                                 @Value("${app.checkin.tolerancia-depois-minutos}") long toleranciaDepoisMinutos) {
        this.sessaoRepository = sessaoRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.checkinService = checkinService;
        this.codigoRotativo = codigoRotativo;
        this.toleranciaAntesMinutos = toleranciaAntesMinutos;
        this.toleranciaDepoisMinutos = toleranciaDepoisMinutos;
    }

    public SessaoCheckin criar(UUID eventoId, TipoSessaoCheckin tipo) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento nao encontrado: " + eventoId));

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime abertura = aberturaDaJanela(evento);
        LocalDateTime fechamento = fechamentoDaJanela(evento);

        // Barrar aqui, e nao so na hora do aluno escanear: o admin esta
        // clicando "Gerar QR code" e precisa saber na hora que aquele QR
        // nao valeria nada.
        if (agora.isBefore(abertura)) {
            throw new RegraNegocioException(
                    "O check-in deste evento so abre em " + abertura.format(HORA) + ".");
        }
        if (agora.isAfter(fechamento)) {
            throw new RegraNegocioException(
                    "O check-in deste evento fechou em " + fechamento.format(HORA) + ".");
        }

        SessaoCheckin sessao = SessaoCheckin.builder()
                .evento(evento)
                .tipo(tipo)
                .segredo(codigoRotativo.gerarSegredo())
                .criadaEm(agora)
                .expiraEm(fechamento)
                .build();
        return sessaoRepository.save(sessao);
    }

    public SessaoCheckin buscarValida(UUID sessaoId) {
        SessaoCheckin sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("QR code invalido"));

        LocalDateTime agora = LocalDateTime.now();
        if (agora.isBefore(aberturaDaJanela(sessao.getEvento()))) {
            throw new RegraNegocioException("O check-in deste evento ainda nao abriu.");
        }
        if (sessao.isExpirada()) {
            throw new RegraNegocioException("O check-in deste evento ja fechou.");
        }
        return sessao;
    }

    /**
     * Confirma a presenca do aluno. O {@code codigo} vem do QR que estava
     * na tela no momento do escaneamento e precisa ser o da janela atual
     * (ou o da anterior) - e o que impede que o link repassado no grupo
     * funcione minutos depois.
     */
    public Checkin confirmar(UUID sessaoId, UUID alunoId, String codigo) {
        SessaoCheckin sessao = buscarValida(sessaoId);

        if (!codigoRotativo.aceita(sessao, codigo)) {
            throw new RegraNegocioException(
                    "Este QR code ja mudou. Escaneie de novo o que esta na tela agora.");
        }

        Inscricao inscricao = inscricaoRepository.findByAlunoIdAndEventoId(alunoId, sessao.getEvento().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Voce nao esta inscrito neste evento"));

        if (inscricao.getStatus() == InscricaoStatus.CANCELADA) {
            throw new RegraNegocioException("Sua inscricao neste evento esta cancelada");
        }

        return sessao.getTipo() == TipoSessaoCheckin.ENTRADA
                ? checkinService.registrarEntrada(inscricao)
                : checkinService.registrarSaida(inscricao);
    }

    /** Um pouco antes do inicio, pra o admin conseguir deixar o QR na
     *  tela enquanto a sala enche. */
    private LocalDateTime aberturaDaJanela(Evento evento) {
        return evento.getDataHoraInicio().minusMinutes(toleranciaAntesMinutos);
    }

    /** Um pouco depois do fim, pra quem esta saindo ainda conseguir
     *  fazer o check-out na porta. */
    private LocalDateTime fechamentoDaJanela(Evento evento) {
        return evento.getDataHoraFim().plusMinutes(toleranciaDepoisMinutos);
    }
}
