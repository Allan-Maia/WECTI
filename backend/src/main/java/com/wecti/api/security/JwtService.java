package com.wecti.api.security;

import com.wecti.api.domain.Perfil;
import com.wecti.api.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_PERFIL = "perfil";

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-ms}") long expiracaoMs) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(chave)
                .compact();
    }

    public Optional<AuthenticatedUser> validarEExtrair(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID id = UUID.fromString(claims.getSubject());
            Perfil perfil = Perfil.valueOf(claims.get(CLAIM_PERFIL, String.class));
            return Optional.of(new AuthenticatedUser(id, perfil));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
