package com.nexaerp.auth;

import com.nexaerp.auth.dto.*;
import com.nexaerp.user.User;

public interface AuthService {
    // Authenticate user and return JWT tokens
    LoginResponseDto login(LoginRequestDto request, String ipAddress, String deviceName);

    // Generate new access token using refresh token
    LoginResponseDto refresh(RefreshTokenRequestDto request);

    // Revoke all refresh tokens for current user (logout)
    void logout(Long userId);


//    for email

    void verifyEmail(String token);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequestDto request);
    void resendVerification(String email);
    void sendVerificationEmail(User user);


    // Validate invite token (check if valid and not expired)
    void validateInviteToken(String token);

    // Set password using invite token
    void setPassword(SetPasswordRequestDto request);

    void sendInviteEmail(User user);

    // Resend invite email
    void resendInvite(String email);
}
