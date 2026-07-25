package com.nexaerp.email;

import com.nexaerp.email.dto.EmailDto;

public interface EmailService {
    void sendEmail(EmailDto email);

    void sendVerificationEmail(String toEmail, String userName, String token);

    void sendPasswordResetEmail(String toEmail, String userName, String token);

    void sendInviteEmail(String toEmail, String userName, String token);
}
