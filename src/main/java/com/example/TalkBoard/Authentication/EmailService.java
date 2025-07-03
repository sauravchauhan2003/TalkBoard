package com.example.TalkBoard.Authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private final JavaMailSender mailSender;
    @Value("{baselink.value}")
    private String baselink;
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationLink(String to, String token) {

        String link = baselink+"/verify?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Email Verification - TalkBoard");
        message.setText("Click this link to verify your account:\n" + link);
        mailSender.send(message);
    }
}
