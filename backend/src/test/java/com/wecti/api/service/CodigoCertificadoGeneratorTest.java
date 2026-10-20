package com.wecti.api.service;

import com.wecti.api.repository.CertificadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Codigo publico de validacao do certificado (ex.: WCT-2026-A7F3K2).
 *
 * As garantias testadas aqui sao de seguranca, nao de formato:
 *  - nao pode repetir (dois certificados com o mesmo codigo tornariam a
 *    validacao ambigua);
 *  - nao pode ser previsivel (codigo sequencial permitiria varrer a
 *    pagina publica e listar todos os alunos certificados);
 *  - nao pode ter caractere ambiguo, porque e digitado a mao a partir de
 *    um papel impresso.
 */
@ExtendWith(MockitoExtension.class)
class CodigoCertificadoGeneratorTest {

    /** O mesmo alfabeto do gerador: sem 0/O e sem 1/I/L. */
    private static final String ALFABETO_ESPERADO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    @Mock private CertificadoRepository certificadoRepository;
    @InjectMocks private CodigoCertificadoGenerator generator;

    @Test
    @DisplayName("formato WCT-{ano}-{6 caracteres}")
    void formato() {
        when(certificadoRepository.existsByCodigo(anyString())).thenReturn(false);

        String codigo = generator.gerar();

        assertThat(codigo).matches("^WCT-" + Year.now().getValue() + "-[" + ALFABETO_ESPERADO + "]{6}$");
    }

    @Test
    @DisplayName("nunca usa caractere ambiguo (0, O, 1, I, L)")
    void semCaractereAmbiguo() {
        when(certificadoRepository.existsByCodigo(anyString())).thenReturn(false);

        Set<Character> usados = new HashSet<>();
        IntStream.range(0, 500).forEach(i -> {
            String sufixo = generator.gerar().substring(9);
            sufixo.chars().forEach(c -> usados.add((char) c));
        });

        assertThat(usados)
                .as("esses caracteres se confundem quando alguem digita olhando o certificado impresso")
                .doesNotContain('0', 'O', '1', 'I', 'L');
        assertThat(usados).allSatisfy(c -> assertThat(ALFABETO_ESPERADO).contains(String.valueOf(c)));
    }

    @Test
    @DisplayName("nao e sequencial nem repetitivo: 500 codigos, 500 valores distintos")
    void naoEPrevisivel() {
        when(certificadoRepository.existsByCodigo(anyString())).thenReturn(false);

        Set<String> codigos = new HashSet<>();
        IntStream.range(0, 500).forEach(i -> codigos.add(generator.gerar()));

        assertThat(codigos)
                .as("codigo previsivel permitiria varrer a pagina publica de validacao")
                .hasSize(500);
    }

    @Test
    @DisplayName("se o codigo sorteado ja existir, sorteia outro")
    void reagePorColisao() {
        // primeira consulta diz que ja existe, a segunda que nao
        when(certificadoRepository.existsByCodigo(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String codigo = generator.gerar();

        assertThat(codigo).isNotNull();
        // duas consultas = houve nova tentativa apos a colisao
        org.mockito.Mockito.verify(certificadoRepository, org.mockito.Mockito.times(2))
                .existsByCodigo(anyString());
    }

    @Test
    @DisplayName("falha alto se nunca conseguir um codigo livre, em vez de repetir ou travar")
    void falhaAposMuitasColisoes() {
        when(certificadoRepository.existsByCodigo(anyString())).thenReturn(true);

        assertThatThrownBy(() -> generator.gerar())
                .as("colisao sempre indica defeito (ex.: gerador quebrado), nao azar")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unico");
    }
}
