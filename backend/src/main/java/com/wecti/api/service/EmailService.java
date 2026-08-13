package com.wecti.api.service;

import com.wecti.api.domain.Inscricao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mail ainda nao configurado neste esqueleto (sem servidor SMTP
 * definido) - apenas loga a intencao de envio. Trocar por um
 * JavaMailSender real quando a infraestrutura de e-mail for definida.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void enviarQrCode(Inscricao inscricao) {
        log.info("Envio de e-mail com QR code para {} (inscricao {}, evento {})",
                inscricao.getAluno().getEmail(), inscricao.getId(), inscricao.getEvento().getTitulo());
    }
}
