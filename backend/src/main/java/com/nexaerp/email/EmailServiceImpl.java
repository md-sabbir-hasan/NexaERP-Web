package com.nexaerp.email;

import com.nexaerp.email.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService{
    private final JavaMailSender mailSender;


    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;


    @Override
    public void sendEmail(EmailDto email) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email.getTo());
        message.setSubject(email.getSubject());
        message.setText(email.getBody());
        mailSender.send(message);

    }

    @Override
    public void sendVerificationEmail(String toEmail, String userName, String token) {

        String verifyLink = frontendUrl + "/verify-email?token=" + token;

        String body = "Dear " + userName + ",\n\n" +
                "Welcome to NexaERP!\n\n" +
                "Please verify your email by clicking the link below:\n\n" +
                verifyLink + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "NexaERP Team";

        sendEmail(EmailDto.builder()
                .to(toEmail)
                .subject("Verify Your Email — NexaERP")
                .body(body)
                .build());

    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        String body = "Dear " + userName + ",\n\n" +
                "We received a request to reset your NexaERP password.\n\n" +
                "Click the link below to reset your password:\n\n" +
                resetLink + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "NexaERP Team";

        sendEmail(EmailDto.builder()
                .to(toEmail)
                .subject("Reset Your Password — NexaERP")
                .body(body)
                .build());

    }

    @Override
    public void sendInviteEmail(String toEmail, String userName, String token) {
        String inviteLink = frontendUrl + "/set-password?token=" + token;

        String body = "Dear " + userName + ",\n\n" +
                "You have been invited to NexaERP!\n\n" +
                "Click the link below to set your password and activate your account:\n\n" +
                inviteLink + "\n\n" +
                "This link will expire in 48 hours.\n\n" +
                "If you did not expect this invitation, please ignore this email.\n\n" +
                "Best regards,\n" +
                "NexaERP Team";

        sendEmail(EmailDto.builder()
                .to(toEmail)
                .subject("You're Invited to NexaERP — Set Your Password")
                .body(body)
                .build());
    }
}
