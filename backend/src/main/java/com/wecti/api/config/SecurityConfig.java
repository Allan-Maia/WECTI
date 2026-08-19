package com.wecti.api.config;

import com.wecti.api.security.JwtAuthenticationFilter;
import com.wecti.api.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Regras reais de autorizacao por perfil (admin/professor/aluno), via JWT
 * stateless. Substitui o permitAll do esqueleto inicial - ver
 * docs/openapi.yaml para o mapeamento endpoint -> perfil combinado com o
 * time.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Sem isso, o navegador bloqueia toda chamada do frontend (origem
     * http://localhost:5173 em dev, ou o dominio de producao do site) pro
     * backend (outra origem/porta) - a politica de CORS e do navegador,
     * "curl" e Postman nao aplicam ela, por isso funcionava testando por
     * fora mas nao pela tela de login de verdade. Configure
     * CORS_ALLOWED_ORIGINS (lista separada por virgula) quando o frontend
     * for pro ar em producao - por padrao so libera o Vite local.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> origensPermitidas) {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origensPermitidas);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracao);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sem isso, requisicao sem token (ou com token invalido/expirado)
                // cai no comportamento padrao do Spring Security - 403 puro, sem
                // o nosso formato de ErroResponse, e sem diferenciar "nao
                // autenticado" (401) de "autenticado mas sem permissao" (403).
                //
                // Escreve o JSON na mao (sem ObjectMapper) de proposito: o Spring
                // Boot 4 deste projeto auto-configura Jackson 3.x
                // (tools.jackson.databind.ObjectMapper), mas o classpath tambem
                // tem o Jackson 2.x classico (com.fasterxml.jackson) por causa da
                // lib do JWT - nao existe bean do tipo classico pra injetar, e o
                // corpo aqui e simples demais pra precisar de Jackson mesmo.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                escreverErro(response, HttpStatus.UNAUTHORIZED,
                                        "nao_autenticado", "Autenticacao necessaria"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                escreverErro(response, HttpStatus.FORBIDDEN,
                                        "acesso_negado", "Perfil sem permissao para esta operacao")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/auth/login", "/auth/registrar", "/auth/redefinir-senha").permitAll()
                        .requestMatchers("/docs/**", "/api-docs/**", "/swagger-ui/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/eventos/*/inscricoes").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.GET, "/eventos/*/inscricoes").hasAnyRole("ADMIN", "PROFESSOR")

                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/usuarios").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.PUT, "/usuarios/*").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()

                        .requestMatchers(HttpMethod.POST, "/periodos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/periodos").authenticated()

                        .requestMatchers(HttpMethod.POST, "/palestrantes").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET, "/palestrantes").authenticated()

                        .requestMatchers(HttpMethod.POST, "/eventos").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.PUT, "/eventos/*").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.DELETE, "/eventos/*").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET, "/eventos", "/eventos/*").authenticated()

                        .requestMatchers("/me/inscricoes").hasRole("ALUNO")
                        .requestMatchers("/me/pontuacao").hasRole("ALUNO")
                        .requestMatchers("/pontuacao/aluno/*/periodo/*").hasAnyRole("ADMIN", "PROFESSOR")

                        .requestMatchers("/inscricoes/*/certificado").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.DELETE, "/inscricoes/*").hasRole("ALUNO")
                        .requestMatchers(HttpMethod.GET, "/inscricoes/*").authenticated()

                        // Check-in por sessao (QR gerado pelo evento, nao mais por aluno):
                        // admin/professor geram o QR de entrada/saida, o proprio aluno
                        // escaneia e confirma - ver CheckinSessaoController.
                        .requestMatchers(HttpMethod.POST, "/eventos/*/checkin-sessoes").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.GET, "/checkin-sessoes/*/qrcode").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.POST, "/checkin-sessoes/*/confirmar").hasRole("ALUNO")

                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escreverErro(jakarta.servlet.http.HttpServletResponse response,
                               HttpStatus status, String erro, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String corpo = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status.value()
                + ",\"erro\":\"" + erro + "\",\"mensagem\":\"" + escaparJson(mensagem) + "\"}";
        response.getWriter().write(corpo);
    }

    private static String escaparJson(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
