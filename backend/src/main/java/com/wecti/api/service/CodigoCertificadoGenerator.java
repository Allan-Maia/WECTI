package com.wecti.api.service;

import com.wecti.api.repository.CertificadoRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Gera o codigo publico de validacao do certificado, no formato
 * WCT-{ano}-{6 caracteres} (ex.: WCT-2026-A7F3K2).
 *
 * Decisoes de design:
 *
 * 1. ALEATORIO, nao sequencial. Um codigo tipo "WCT-2026-000001",
 *    "...000002" permitiria a qualquer pessoa varrer a pagina publica de
 *    validacao e listar nome/evento de todos os alunos certificados. Com
 *    sorteio aleatorio, so consegue validar quem ja tem o codigo em maos
 *    (ou seja, quem recebeu o certificado).
 *
 * 2. SecureRandom, nao Random. Random e previsivel a partir de poucas
 *    saidas observadas (gerador linear congruente com seed adivinhavel) -
 *    alguem com dois ou tres certificados em maos poderia derivar os
 *    proximos. SecureRandom nao tem esse problema.
 *
 * 3. Alfabeto sem caracteres ambiguos: fora 0/O, 1/I/L. Esse codigo e
 *    feito pra ser DIGITADO por uma pessoa lendo um papel impresso; nao
 *    da pra deixar duvida entre zero e a letra O.
 *
 * 4. 6 caracteres em um alfabeto de 31 = 31^6, ~887 milhoes de
 *    combinacoes. Inviavel de chutar por tentativa e erro, e curto o
 *    suficiente pra digitar sem erro.
 *
 * 5. Unicidade em duas camadas: aqui re-sorteia enquanto o codigo ja
 *    existir, e a coluna tem constraint UNIQUE no banco como rede de
 *    seguranca (ver V4__codigo_validacao_certificado.sql). Essa checagem
 *    sozinha nao e atomica - duas emissoes simultaneas poderiam sortear
 *    o mesmo codigo e passar as duas pela verificacao; nesse caso o banco
 *    recusa a segunda em vez de gravar duplicado.
 */
@Component
public class CodigoCertificadoGenerator {

    private static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int TAMANHO = 6;
    private static final int MAX_TENTATIVAS = 20;

    private final SecureRandom random = new SecureRandom();
    private final CertificadoRepository certificadoRepository;

    public CodigoCertificadoGenerator(CertificadoRepository certificadoRepository) {
        this.certificadoRepository = certificadoRepository;
    }

    public String gerar() {
        for (int tentativa = 0; tentativa < MAX_TENTATIVAS; tentativa++) {
            String codigo = "WCT-" + Year.now().getValue() + "-" + sortearSufixo();
            if (!certificadoRepository.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        // Praticamente impossivel com ~887 milhoes de combinacoes: se
        // acontecer, e sinal de defeito (ex.: SecureRandom quebrado
        // devolvendo sempre o mesmo valor), nao de azar - falhar alto e
        // melhor do que gravar codigo duplicado ou entrar em loop eterno.
        throw new IllegalStateException(
                "Nao foi possivel gerar um codigo de certificado unico apos " + MAX_TENTATIVAS + " tentativas");
    }

    private String sortearSufixo() {
        StringBuilder sb = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
