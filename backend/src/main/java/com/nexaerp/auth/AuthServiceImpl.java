package com.nexaerp.auth;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.auth.dto.*;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.email.EmailService;
import com.nexaerp.passwordreset.PasswordResetToken;
import com.nexaerp.passwordreset.PasswordResetTokenRepository;
import com.nexaerp.security.JwtUtil;
import com.nexaerp.token.RefreshToken;
import com.nexaerp.token.RefreshTokenRepository;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.verification.EmailVerification;
import com.nexaerp.verification.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;


    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private int lockDurationMinutes;


    @Override
    public LoginResponseDto login(LoginRequestDto request, String ipAddress, String deviceName) {
        // find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        // Check lock
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null &&
                    LocalDateTime.now().isBefore(user.getLockedUntil())) {
                throw new BusinessRuleException(
                        "Account is locked. Try again after " + user.getLockedUntil());
            } else {
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active");
        }

        // Wrong password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            }

            userRepository.save(user); //

            if (user.getStatus() == UserStatus.LOCKED) {
                throw new BusinessRuleException(
                        "Account locked due to too many failed attempts. Try again after " +
                                lockDurationMinutes + " minutes");
            }

            throw new BusinessRuleException("Invalid email or password");
        }

        // Success
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // collect all permissions from all roles
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());

        //  generate access token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), permissions);

        //  generate and save refresh token
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .ipAddress(ipAddress)
                .deviceName(deviceName)
                .build();

        refreshTokenRepository.save(refreshToken);


        // audit
        auditLogService.log(
                AuditAction.LOGIN,
                "USER",
                user.getId(),
                null,
                user.getEmail()
        );

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public LoginResponseDto refresh(RefreshTokenRequestDto request) {
        // Find the refresh token in DB
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        // Check if token is revoked
        if (refreshToken.getRevoked()) {
            throw new BusinessRuleException("Refresh token has been revoked");
        }

        // Check if token is expired
        if (LocalDateTime.now().isAfter(refreshToken.getExpiresAt())) {
            throw new BusinessRuleException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        // Collect permission
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), permissions);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken()) // same refresh token
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        // Revoke all refresh token for this user
        refreshTokenRepository.deleteAllByUserId(userId);
    }



    // ============Email Verification=============

    @Override
    @Transactional
    public void verifyEmail(String token) {

        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Invalid verification token"));

        // Check if already verified
        if (verification.getVerified()) {
            throw new BusinessRuleException("Email already verified");
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            throw new BusinessRuleException("Verification token has expired. Please request a new one.");
        }

        // Activate user
        User user = verification.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Mark as verified
        verification.setVerified(true);
        emailVerificationRepository.save(verification);

    }

    @Override
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessRuleException("Account is inactive");
        }

        // Delete old reset token
        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);

    }

    @Override
    public void resetPassword(ResetPasswordRequestDto request) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid reset token"));

        // Check if already used
        if (resetToken.getUsed()) {
            throw new BusinessRuleException("Reset token already used");
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
            throw new BusinessRuleException("Reset token has expired");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("Password and Confirm Password do not match");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens for security
        refreshTokenRepository.deleteAllByUserId(user.getId());

    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("Email already verified");
        }

        sendVerificationEmail(user);
    }

    // Called when user is created
    @Override
    public void sendVerificationEmail(User user) {

        // Delete old token if exists
        emailVerificationRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .verified(false)
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);

    }

    @Override
    public void validateInviteToken(String token) {
        User user = userRepository.findByInviteToken(token)
                .orElseThrow(() -> new BusinessRuleException("Invalid invite token"));

        if (LocalDateTime.now().isAfter(user.getInviteExpiry())) {
            throw new BusinessRuleException(
                    "Invite link has expired. Please ask admin to resend.");
        }

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("Account already activated");
        }
    }

    @Override
    @Transactional
    public void setPassword(SetPasswordRequestDto request) {
        // Validate token
        User user = userRepository.findByInviteToken(request.getInviteToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid invite token"));

        if (LocalDateTime.now().isAfter(user.getInviteExpiry())) {
            throw new BusinessRuleException("Invite link has expired");
        }

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("Account already activated");
        }

        // Password confirm check
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }

        // Set password and activate account
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setInviteToken(null);    // Token clear করো
        user.setInviteExpiry(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

    }

    @Override
    @Transactional
    public void resendInvite(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessRuleException("Account already activated");
        }

        sendInviteEmail(user);
    }

    @Override
    public void sendInviteEmail(User user) {
        String token = UUID.randomUUID().toString();

        user.setInviteToken(token);
        user.setInviteExpiry(LocalDateTime.now().plusHours(48));
        userRepository.save(user);

        emailService.sendInviteEmail(user.getEmail(), user.getName(), token);

    }
}
