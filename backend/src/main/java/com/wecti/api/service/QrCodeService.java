package com.wecti.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeService {

    private static final int TAMANHO_PX = 300;

    public byte[] gerarPng(String conteudo) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, TAMANHO_PX, TAMANHO_PX);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", saida);
            return saida.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new RuntimeException("Falha ao gerar QR code", ex);
        }
    }
}
