package com.wecti.api.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Gera o certificado em PDF renderizando o template XHTML/CSS
 * (certificado/certificado.xhtml) - copia fiel do layout aprovado, com as
 * fontes e o logo SVG originais.
 *
 * Renderizar o HTML aprovado, em vez de redesenhar o layout na mao com a
 * API do PDFBox, e o que garante que o PDF saia igual ao modelo validado:
 * as mesmas fontes (Anton, Inter, IBM Plex Mono), o mesmo logo vetorial e
 * as mesmas medidas, sem uma segunda "versao aproximada" do layout pra
 * manter em sincronia.
 *
 * As fontes vivem em certificado/fontes (SIL OFL, que permite embutir em
 * documento) e sao registradas aqui; o proprio renderizador embute no PDF
 * apenas os glifos usados, entao o arquivo final continua pequeno.
 */
@Component
public class CertificadoPdfRenderer {

    private static final String TEMPLATE = "certificado/certificado.xhtml";
    private static final String DIR_FONTES = "certificado/fontes/";

    /**
     * Largura media de um caractere do nome, em milimetros, com Anton em
     * 56px - medido no proprio modelo renderizado no navegador. Serve pra
     * encolher o nome ate caber na faixa de 201mm reservada pra ele, do
     * mesmo jeito que o layout original faz visualmente.
     */
    private static final double MM_POR_CARACTERE_56PX = 6.45;
    private static final double LARGURA_NOME_MM = 201.0;
    private static final double TAMANHO_NOME_PADRAO = 56.0;
    private static final double TAMANHO_NOME_MINIMO = 30.0;

    private static final String[] MESES = {
            "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    };

    public byte[] renderizar(DadosCertificado dados, byte[] qrCodePng) {
        double tamanhoNome = tamanhoDoNome(dados.alunoNome());

        String html = carregarTemplate()
                .replace("{{NOME_FONT_SIZE}}", formatar(tamanhoNome))
                .replace("{{NOME_LINE_HEIGHT}}", formatar(tamanhoNome * 0.92))
                .replace("{{NOME}}", escapar(dados.alunoNome().toUpperCase()))
                .replace("{{RGM}}", escapar(dados.alunoRgm()))
                .replace("{{CARGA}}", escapar(dados.cargaHoraria()))
                .replace("{{DATA}}", escapar(dados.dataRealizacao()))
                .replace("{{EVENTO}}", escapar(dados.eventoTitulo()))
                .replace("{{CODIGO}}", escapar(dados.codigo()))
                .replace("{{URL_VALIDACAO}}", escapar(dados.urlValidacao()))
                .replace("{{QR_DATA_URI}}", "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodePng));

        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // Desenha os SVGs inline do layout (faixa do topo, asteriscos
            // e o logo da UNICID).
            builder.useSVGDrawer(new BatikSVGDrawer());
            registrarFontes(builder);
            builder.withW3cDocument(parsearXhtml(html), "");
            builder.toStream(saida);
            builder.run();
            return saida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gerar certificado em PDF", ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Falha ao gerar certificado em PDF", ex);
        }
    }

    /**
     * Nome comprido encolhe ate caber na linha, como no layout original.
     * Abaixo do tamanho minimo para de encolher e deixa quebrar em duas
     * linhas (o bloco tem largura fixa), pra nao virar letra ilegivel.
     */
    private double tamanhoDoNome(String nome) {
        double larguraEstimada = nome.length() * MM_POR_CARACTERE_56PX;
        if (larguraEstimada <= LARGURA_NOME_MM) {
            return TAMANHO_NOME_PADRAO;
        }
        double ajustado = TAMANHO_NOME_PADRAO * LARGURA_NOME_MM / larguraEstimada;
        return Math.max(TAMANHO_NOME_MINIMO, ajustado);
    }

    private void registrarFontes(PdfRendererBuilder builder) {
        // (arquivo, familia CSS, peso)
        Map<String, Object[]> fontes = Map.of(
                "Anton-Regular.ttf", new Object[]{"Anton", 400},
                "Inter-Regular.ttf", new Object[]{"Inter", 400},
                "Inter-SemiBold.ttf", new Object[]{"Inter", 600},
                "Inter-Bold.ttf", new Object[]{"Inter", 700},
                "IBMPlexMono-SemiBold.ttf", new Object[]{"IBM Plex Mono", 600});

        fontes.forEach((arquivo, meta) -> builder.useFont(
                () -> abrir(DIR_FONTES + arquivo),
                (String) meta[0],
                (Integer) meta[1],
                PdfRendererBuilder.FontStyle.NORMAL,
                true));
    }

    private InputStream abrir(String caminho) {
        try {
            return new ClassPathResource(caminho).getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException("Recurso do certificado nao encontrado: " + caminho, ex);
        }
    }

    private String carregarTemplate() {
        try (InputStream in = abrir(TEMPLATE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao ler o template do certificado", ex);
        }
    }

    /**
     * O renderizador trabalha sobre um DOM do W3C e o template inclui SVG,
     * que so funciona com o parser ciente de namespace. Entidades externas
     * ficam desligadas (o template e nosso, mas nao ha motivo pra deixar
     * essa porta aberta).
     */
    private Document parsearXhtml(String html) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return fabrica.newDocumentBuilder()
                    .parse(new InputSource(new java.io.StringReader(html)));
        } catch (Exception ex) {
            throw new IllegalStateException("Template do certificado nao e XHTML valido", ex);
        }
    }

    /** Escapa o que vai pro XHTML - nome de aluno e titulo de evento sao
     *  texto livre e podem conter &, <, > ou aspas. */
    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String formatar(double valor) {
        return String.format(java.util.Locale.US, "%.2f", valor);
    }

    static String formatarData(java.time.LocalDate data) {
        return String.format("%02d %s %d", data.getDayOfMonth(), MESES[data.getMonthValue() - 1], data.getYear());
    }

    /** Dados ja prontos pra impressao - o renderer nao conhece entidades. */
    public record DadosCertificado(
            String alunoNome,
            String alunoRgm,
            String eventoTitulo,
            String dataRealizacao,
            String cargaHoraria,
            String codigo,
            String urlValidacao) {
    }
}
