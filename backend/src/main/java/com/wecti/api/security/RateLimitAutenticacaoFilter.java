package com.wecti.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trava tentativa de adivinhar senha por forca bruta nas rotas de
 * autenticacao.
 *
 * Conta apenas as tentativas que FALHAM (401). Isso e o ponto central do
 * desenho: numa palestra, a turma inteira acessa pelo Wi-Fi do campus e
 * sai toda com o MESMO IP publico. Se o contador somasse todo acesso, 80
 * alunos entrando corretamente estourariam o limite e bloqueariam uns aos
 * outros. Contando so falha, quem digita a senha certa nunca entra na
 * conta - e quem fica errando repetidamente, sim.
 *
 * O estado fica em memoria, o que basta para a hospedagem atual (uma
 * instancia). Se um dia a aplicacao rodar em mais de uma instancia, cada
 * uma tera seu proprio contador e o limite efetivo sera multiplicado -
 * nesse cenario isso precisa migrar para um armazenamento compartilhado
 * (Redis, por exemplo).
 *
 * Importante: o IP vem de request.getRemoteAddr(). Atras de proxy/CDN,
 * isso so devolve o IP real do visitante se
 * server.forward-headers-strategy estiver ligado (ver application.yml) -
 * sem isso, TODAS as requisicoes chegariam com o IP do proxy e um unico
 * atacante bloquearia o site inteiro. Nao lemos X-Forwarded-For na mao de
 * proposito: esse cabecalho e forjavel por quem chama, e confiar nele sem
 * o filtro do Spring por tras permitiria burlar o limite trocando o valor
 * a cada tentativa.
 */
@Component
public class RateLimitAutenticacaoFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAutenticacaoFilter.class);

    private static final Set<String> ROTAS_PROTEGIDAS = Set.of(
            "/auth/login", "/auth/redefinir-senha", "/auth/registrar");

    /** Limpa registros vencidos quando o mapa passa desse tamanho, pra
     *  memoria nao crescer sem limite com IPs que nunca mais voltaram. */
    private static final int LIMITE_PARA_LIMPEZA = 1_000;

    private final int maxFalhas;
    private final Duration janela;
    private final Map<String, Tentativas> porIp = new ConcurrentHashMap<>();

    public RateLimitAutenticacaoFilter(
            @Value("${app.rate-limit.max-falhas:10}") int maxFalhas,
            @Value("${app.rate-limit.janela-minutos:15}") long janelaMinutos) {
        this.maxFalhas = maxFalhas;
        this.janela = Duration.ofMinutes(janelaMinutos);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && ROTAS_PROTEGIDAS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        Tentativas tentativas = porIp.get(ip);

        if (tentativas != null && tentativas.bloqueado(maxFalhas, janela)) {
            long faltamSegundos = tentativas.segundosAteLiberar(janela);
            log.warn("Rate limit: {} bloqueado em {} ({} falhas); libera em {}s",
                    ip, request.getRequestURI(), tentativas.contador.get(), faltamSegundos);
            responderBloqueado(response, faltamSegundos);
            return;
        }

        chain.doFilter(request, response);

        // 401 aqui e sempre credencial que nao confere (essas rotas sao
        // publicas, entao nao ha 401 por "falta token").
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            registrarFalha(ip);
        }
    }

    private void registrarFalha(String ip) {
        if (porIp.size() > LIMITE_PARA_LIMPEZA) {
            porIp.values().removeIf(t -> t.expirou(janela));
        }
        porIp.compute(ip, (chave, atual) -> {
            if (atual == null || atual.expirou(janela)) {
                return new Tentativas();
            }
            atual.contador.incrementAndGet();
            return atual;
        });
    }

    /** Mesmo formato de erro do resto da API (ver GlobalExceptionHandler),
     *  escrito na mao porque este filtro roda antes do Spring MVC. */
    private void responderBloqueado(HttpServletResponse response, long faltamSegundos) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(faltamSegundos));
        long minutos = Math.max(1, faltamSegundos / 60);
        response.getWriter().write("{\"timestamp\":\"" + Instant.now()
                + "\",\"status\":429,\"erro\":\"muitas_tentativas\",\"mensagem\":\""
                + "Muitas tentativas seguidas. Aguarde " + minutos
                + " minuto(s) e tente de novo.\",\"campo\":null}");
    }

    private static final class Tentativas {
        private final AtomicInteger contador = new AtomicInteger(1);
        private final Instant primeiraFalha = Instant.now();

        boolean expirou(Duration janela) {
            return Instant.now().isAfter(primeiraFalha.plus(janela));
        }

        boolean bloqueado(int maxFalhas, Duration janela) {
            return !expirou(janela) && contador.get() >= maxFalhas;
        }

        long segundosAteLiberar(Duration janela) {
            return Math.max(1, Duration.between(Instant.now(), primeiraFalha.plus(janela)).toSeconds());
        }
    }
}
