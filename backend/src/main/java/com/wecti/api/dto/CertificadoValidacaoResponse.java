package com.wecti.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resposta da pagina PUBLICA de validacao (GET /validar/{codigo}).
 *
 * Traz so o necessario pra provar autenticidade: quem participou, de que
 * evento, quando e com que carga horaria - exatamente os dados que ja
 * estao impressos no certificado que a pessoa tem em maos.
 *
 * NAO expoe RGM de proposito. O endpoint e aberto (sem login, porque quem
 * valida costuma ser um recrutador ou outra instituicao, que nao tem
 * conta no sistema), entao tudo que estiver aqui e publico pra quem tiver
 * o codigo. O RGM e identificador academico interno e nao acrescenta nada
 * a verificacao - ele aparece so no PDF, que o proprio aluno decide pra
 * quem enviar.
 */
public record CertificadoValidacaoResponse(
        String codigo,
        String alunoNome,
        String eventoTitulo,
        LocalDate dataRealizacao,
        String cargaHoraria,
        LocalDateTime emitidoEm) {
}
