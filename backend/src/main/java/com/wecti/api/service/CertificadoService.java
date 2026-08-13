package com.wecti.api.service;

import com.wecti.api.domain.Certificado;
import com.wecti.api.domain.Checkin;
import com.wecti.api.domain.Evento;
import com.wecti.api.domain.Inscricao;
import com.wecti.api.exception.RegraNegocioException;
import com.wecti.api.repository.CertificadoRepository;
import com.wecti.api.repository.CheckinRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@Service
public class CertificadoService {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(new Locale("pt", "BR"));

    private final CheckinRepository checkinRepository;
    private final CertificadoRepository certificadoRepository;

    public CertificadoService(CheckinRepository checkinRepository, CertificadoRepository certificadoRepository) {
        this.checkinRepository = checkinRepository;
        this.certificadoRepository = certificadoRepository;
    }

    public byte[] emitir(Inscricao inscricao) {
        Evento evento = inscricao.getEvento();
        Checkin checkin = checkinRepository.findByInscricaoId(inscricao.getId())
                .orElseThrow(() -> new RegraNegocioException(
                        "Criterios de presenca ainda nao cumpridos (check-in/check-out pendentes)"));

        if (!checkin.isPresencaQualificada(evento.getDataHoraInicio(), evento.getDataHoraFim())) {
            throw new RegraNegocioException(
                    "Criterios de presenca ainda nao cumpridos (permanencia minima de 75% nao atingida)");
        }

        certificadoRepository.findByInscricaoId(inscricao.getId())
                .orElseGet(() -> certificadoRepository.save(Certificado.builder().inscricao(inscricao).build()));

        return gerarPdf(inscricao);
    }

    private byte[] gerarPdf(Inscricao inscricao) {
        Evento evento = inscricao.getEvento();
        String aluno = sanitizarParaPdf(inscricao.getAluno().getNome());
        String titulo = sanitizarParaPdf(evento.getTitulo());

        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);

            PDType1Font fonteTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fonteTexto = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(fonteTitulo, 22);
                conteudo.newLineAtOffset(60, 750);
                conteudo.showText("Certificado de Participacao");
                conteudo.endText();

                conteudo.beginText();
                conteudo.setFont(fonteTexto, 12);
                conteudo.newLineAtOffset(60, 690);
                conteudo.setLeading(20f);
                conteudo.showText("Certificamos que " + aluno);
                conteudo.newLine();
                conteudo.showText("participou do evento \"" + titulo + "\",");
                conteudo.newLine();
                conteudo.showText("realizado em " + evento.getDataHoraInicio().toLocalDate().format(FORMATO_DATA) + ",");
                conteudo.newLine();
                conteudo.showText("com carga horaria correspondente e valor de " + evento.getPontos() + " pontos,");
                conteudo.newLine();
                conteudo.showText("cumprindo os criterios minimos de presenca exigidos pelo WECTI.");
                conteudo.endText();
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao gerar certificado em PDF", ex);
        }
    }

    /**
     * As fontes padrao do PDFBox (Helvetica) so cobrem Latin-1/WinAnsi -
     * um caractere fora disso (emoji, por exemplo) em nome de aluno ou
     * titulo de evento faz o PDFBox lancar IllegalArgumentException e
     * derrubar a geracao do certificado. Troca por "?" em vez de crashar.
     */
    private static String sanitizarParaPdf(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            sb.append(c <= 0xFF ? c : '?');
        }
        return sb.toString();
    }
}
