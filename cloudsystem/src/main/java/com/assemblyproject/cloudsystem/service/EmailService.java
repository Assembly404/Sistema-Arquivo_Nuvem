package com.assemblyproject.cloudsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:Arquivo Nuvem <no-reply@arquivonuvem.local>}")
    private String from;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    public EmailService(@org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp, long validityMinutes) {
        String subject = "Código de reposição — Arquivo Nuvem";
        String body = "O seu código de reposição é: " + otp
                + "\nVálido por " + validityMinutes + " minutos."
                + "\nSe não foste tu, ignora esta mensagem.";
        if (!enabled || mailSender == null) {
            log.info("[forgot] email OTP para {} (modo dev, envio desativado)", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
