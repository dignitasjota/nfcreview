package com.reviewtap.auth;

import com.reviewtap.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envío de emails transaccionales. Sin SMTP configurado, {@link #isConfigured()} es falso y no se envía nada. */
@Slf4j
@Service
public class MailService {

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String host;

    public MailService(ObjectProvider<JavaMailSender> sender, AppProperties props,
            @Value("${spring.mail.host:}") String host) {
        this.sender = sender;
        this.host = host == null ? "" : host.trim();
        this.from = props.mail() == null || props.mail().from() == null ? "" : props.mail().from().trim();
    }

    public boolean isConfigured() {
        return !host.isBlank() && !from.isBlank() && sender.getIfAvailable() != null;
    }

    /** @return {@code true} si se ha enviado. Nunca lanza: el fallo de correo no debe romper el flujo. */
    public boolean send(String to, String subject, String body) {
        if (!isConfigured()) {
            return false;
        }
        JavaMailSender mailSender = sender.getIfAvailable();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el email '{}': {}", subject, e.getMessage());
            return false;
        }
    }
}
