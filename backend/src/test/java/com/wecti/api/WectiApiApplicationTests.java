package com.wecti.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que a aplicacao inteira sobe: todos os beans se resolvem, sem
 * dependencia faltando nem configuracao quebrada.
 *
 * Parece um teste bobo, mas e o que teria pego o erro de ObjectMapper que
 * derrubou o boot em producao - um bean que nao existia no classpath e so
 * estourava na hora de subir.
 *
 * Roda sobre H2 em memoria (ver application-test.yml), entao nao precisa
 * de MySQL nem de variavel de ambiente para rodar.
 */
@SpringBootTest
@ActiveProfiles("test")
class WectiApiApplicationTests {

    @Test
    @DisplayName("o contexto da aplicacao carrega sem erro")
    void contextLoads() {
    }
}
